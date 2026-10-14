// VieNeu v3 Nano adapter. Algorithm: pnnbao97/VieNeu-TTS v3nano.py (Apache-2.0).
#define ORT_API_MANUAL_INIT
#include "onnxruntime_cxx_api.h"
#include <jni.h>
#include <dlfcn.h>
#include <atomic>
#include <algorithm>
#include <cmath>
#include <memory>
#include <mutex>
#include <random>
#include <stdexcept>
#include <string>
#include <vector>
extern "C" {void* sea_g2p_open(const char*);void sea_g2p_close(void*);char* sea_g2p_phonemize(const void*,const char*,int);void sea_g2p_string_free(char*);const char* sea_g2p_last_error();}
static std::once_flag ortInit;
static void initializeOrt(){std::call_once(ortInit,[]{auto lib=dlopen("libonnxruntime.so",RTLD_NOW|RTLD_LOCAL);if(!lib)throw std::runtime_error("ONNX runtime unavailable");auto get=(const OrtApiBase*(*)())dlsym(lib,"OrtGetApiBase");if(!get)throw std::runtime_error("ONNX C API unavailable");auto api=get()->GetApi(ORT_API_VERSION);if(!api)throw std::runtime_error("ONNX runtime too old");Ort::InitApi(api);});}
static void fail(JNIEnv* e,const char* s){if(!e->ExceptionCheck())e->ThrowNew(e->FindClass("java/lang/IllegalStateException"),s);}
static std::string bytes(JNIEnv*e,jbyteArray a){if(!a)throw std::runtime_error("Missing UTF8 input");std::string s(e->GetArrayLength(a),'\0');e->GetByteArrayRegion(a,0,s.size(),reinterpret_cast<jbyte*>(s.data()));if(s.find('\0')!=std::string::npos)throw std::runtime_error("NUL in UTF8 input");return s;}
static std::vector<float> floats(JNIEnv*e,jfloatArray a,size_t required){if(!a || (size_t)e->GetArrayLength(a)!=required)throw std::runtime_error("Invalid voice tensor shape");std::vector<float> f(required);e->GetFloatArrayRegion(a,0,required,f.data());for(float x:f)if(!std::isfinite(x))throw std::runtime_error("Nonfinite voice tensor");return f;}
struct Nano {
 Ort::Env env{ORT_LOGGING_LEVEL_WARNING,"VieNeu"};Ort::MemoryInfo mem{Ort::MemoryInfo::CreateCpu(OrtArenaAllocator,OrtMemTypeDefault)};
 Ort::SessionOptions options;std::unique_ptr<Ort::Session> text,dur,ve,dec;std::vector<float> nullSpk,nullStyle,nullCtx;std::vector<int64_t> nullShape;void* g2p=nullptr;std::atomic<bool> canceled{false};
 Nano(const std::string&p,std::vector<float>spk,std::vector<float>style):nullSpk(std::move(spk)),nullStyle(std::move(style)){
  options.SetIntraOpNumThreads(2);options.SetInterOpNumThreads(1);options.SetExecutionMode(ExecutionMode::ORT_SEQUENTIAL);options.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);options.AddConfigEntry("session.intra_op.allow_spinning","0");
  auto open=[&](const char*n){return std::make_unique<Ort::Session>(env,(p+"/"+n).c_str(),options);};
  text=open("text_encoder.onnx");dur=open("duration_predictor.onnx");ve=open("vector_estimator.onnx");dec=open("codec_decoder.onnx");
  std::vector<int64_t> ids{1,2};auto ctx=encode(ids,nullStyle);nullShape=ctx.GetTensorTypeAndShapeInfo().GetShape();auto count=ctx.GetTensorTypeAndShapeInfo().GetElementCount();nullCtx.assign(ctx.GetTensorData<float>(),ctx.GetTensorData<float>()+count);
  g2p=sea_g2p_open((p+"/sea_g2p.bin").c_str());if(!g2p)throw std::runtime_error("Cannot open Vietnamese phoneme dictionary");
 }
 ~Nano(){if(g2p)sea_g2p_close(g2p);}
 template<class T>Ort::Value tensor(T*d,size_t n,std::vector<int64_t>shape){return Ort::Value::CreateTensor<T>(mem,d,n,shape.data(),shape.size());}
 Ort::Value encode(std::vector<int64_t>&ids,std::vector<float>&style){std::vector<Ort::Value> in;in.push_back(tensor(ids.data(),ids.size(),{1,(int64_t)ids.size()}));in.push_back(tensor(style.data(),style.size(),{1,50,256}));const char*names[]={"ids","style"},*outputs[]={"ctx"};return std::move(text->Run(Ort::RunOptions{nullptr},names,in.data(),in.size(),outputs,1)[0]);}
 std::vector<float> estimate(std::vector<float>&x,int64_t T,float t,float*ctx,const std::vector<int64_t>&shape,bool*mask,size_t L,std::vector<float>&spk,std::vector<float>&style){
  std::vector<Ort::Value> in;in.push_back(tensor(x.data(),x.size(),{1,144,T}));in.push_back(tensor(&t,1,{1}));in.push_back(tensor(ctx,shape[1]*shape[2],shape));in.push_back(tensor(mask,L,{1,(int64_t)L}));in.push_back(tensor(spk.data(),spk.size(),{1,192}));in.push_back(tensor(style.data(),style.size(),{1,50,256}));
  const char*names[]={"x","t","ctx","ctx_mask","spk","style"};Ort::AllocatorWithDefaultOptions alloc;auto name=ve->GetOutputNameAllocated(0,alloc);const char*out[]={name.get()};auto v=ve->Run(Ort::RunOptions{nullptr},names,in.data(),in.size(),out,1);auto n=v[0].GetTensorTypeAndShapeInfo().GetElementCount();if(n!=x.size())throw std::runtime_error("Invalid flow output");return {v[0].GetTensorData<float>(),v[0].GetTensorData<float>()+n};
 }
 std::vector<float> generate(std::vector<int64_t>ids,std::vector<float>spk,std::vector<float>style,float speed,int steps){
  if(ids.size()<3 || ids.size()>1024 || steps<1 || steps>16 || speed<.5f || speed>2.f)throw std::runtime_error("Invalid synthesis parameters");canceled=false;
  auto ctx=encode(ids,style);auto shape=ctx.GetTensorTypeAndShapeInfo().GetShape();auto L=ids.size();auto mask=std::make_unique<bool[]>(L);for(size_t i=0;i<L;i++)mask[i]=ids[i]!=0;
  std::vector<Ort::Value> in;in.push_back(tensor(ctx.GetTensorMutableData<float>(),shape[1]*shape[2],shape));in.push_back(tensor(mask.get(),L,{1,(int64_t)L}));in.push_back(tensor(spk.data(),spk.size(),{1,192}));
  const char*names[]={"ctx","ctx_mask","spk"};Ort::AllocatorWithDefaultOptions alloc;auto dn=dur->GetOutputNameAllocated(0,alloc);const char*out[]={dn.get()};auto d=dur->Run(Ort::RunOptions{nullptr},names,in.data(),in.size(),out,1);float logSeconds=d[0].GetTensorData<float>()[0];if(!std::isfinite(logSeconds))throw std::runtime_error("Invalid duration");
  auto T=std::max<int64_t>(2,std::llround(std::min(15.,std::exp((double)logSeconds)/speed)*15.625));std::vector<float>x(144*T);std::mt19937 rng(std::random_device{}());std::normal_distribution<float>norm;for(auto&f:x)f=norm(rng);bool nullMask[2]={true,true};
  auto grid=[](double u){return u-(std::cos(1.5707963267948966*u)-1.+u);};
  for(int i=0;i<steps;i++){if(canceled)throw std::runtime_error("Synthesis canceled");float t=grid((double)i/steps),dt=grid((double)(i+1)/steps)-t;auto v=estimate(x,T,t,ctx.GetTensorMutableData<float>(),shape,mask.get(),L,spk,style);if(canceled)throw std::runtime_error("Synthesis canceled");auto vu=estimate(x,T,t,nullCtx.data(),nullShape,nullMask,2,nullSpk,nullStyle);for(size_t j=0;j<x.size();j++)x[j]+=dt*(vu[j]+3.f*(v[j]-vu[j]));}
  if(canceled)throw std::runtime_error("Synthesis canceled");std::vector<Ort::Value> din;din.push_back(tensor(x.data(),x.size(),{1,144,T}));const char*nx[]={"x"};auto on=dec->GetOutputNameAllocated(0,alloc);const char*no[]={on.get()};auto audio=dec->Run(Ort::RunOptions{nullptr},nx,din.data(),1,no,1);auto n=audio[0].GetTensorTypeAndShapeInfo().GetElementCount();if(n==0 || n>24000*16)throw std::runtime_error("Invalid audio length");std::vector<float>w(audio[0].GetTensorData<float>(),audio[0].GetTensorData<float>()+n);for(auto&f:w){if(!std::isfinite(f))throw std::runtime_error("Nonfinite audio");f=std::clamp(f,-1.f,1.f);}auto fade=std::min<size_t>(360,w.size()/2);for(size_t i=0;i<fade;i++){float a=.5f-.5f*std::cos(3.141592653589793*i/std::max<size_t>(1,fade-1));w[i]*=a;w[w.size()-1-i]*=a;}return w;
 }
};
#define JNI_NAME(n) Java_ireader_domain_services_tts_1service_v2_offline_VieNeuNative_##n
extern "C" JNIEXPORT jlong JNICALL JNI_NAME(open)(JNIEnv*e,jclass,jbyteArray path,jfloatArray spk,jfloatArray style){try{initializeOrt();return reinterpret_cast<jlong>(new Nano(bytes(e,path),floats(e,spk,192),floats(e,style,12800)));}catch(const std::exception&x){fail(e,x.what());return 0;}}
extern "C" JNIEXPORT jbyteArray JNICALL JNI_NAME(phonemes)(JNIEnv*e,jclass,jlong h,jbyteArray text){try{auto*r=reinterpret_cast<Nano*>(h);if(!r)throw std::runtime_error("Runtime closed");auto input=bytes(e,text);auto*p=sea_g2p_phonemize(r->g2p,input.c_str(),1);if(!p)throw std::runtime_error("Cannot phonemize text");std::string s(p);sea_g2p_string_free(p);auto a=e->NewByteArray(s.size());e->SetByteArrayRegion(a,0,s.size(),reinterpret_cast<const jbyte*>(s.data()));return a;}catch(const std::exception&x){fail(e,x.what());return nullptr;}}
extern "C" JNIEXPORT jfloatArray JNICALL JNI_NAME(generate)(JNIEnv*e,jclass,jlong h,jlongArray ids,jfloatArray spk,jfloatArray style,jfloat speed,jint steps){try{auto*r=reinterpret_cast<Nano*>(h);if(!r || !ids)throw std::runtime_error("Runtime closed");std::vector<int64_t>tokens(e->GetArrayLength(ids));e->GetLongArrayRegion(ids,0,tokens.size(),reinterpret_cast<jlong*>(tokens.data()));auto audio=r->generate(std::move(tokens),floats(e,spk,192),floats(e,style,12800),speed,steps);auto a=e->NewFloatArray(audio.size());e->SetFloatArrayRegion(a,0,audio.size(),audio.data());return a;}catch(const std::exception&x){fail(e,x.what());return nullptr;}}
extern "C" JNIEXPORT void JNICALL JNI_NAME(abort)(JNIEnv*,jclass,jlong h){if(h)reinterpret_cast<Nano*>(h)->canceled=true;}
extern "C" JNIEXPORT void JNICALL JNI_NAME(close)(JNIEnv*,jclass,jlong h){delete reinterpret_cast<Nano*>(h);}
