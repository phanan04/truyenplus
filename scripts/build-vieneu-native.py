#!/usr/bin/env python3
"""Build VieNeu JNI against the existing ONNX Runtime C ABI; models stay external.
Requires Rust1.89+ with aarch64-linux-android and x86_64-linux-android targets, Android NDK28+.
Local toolchains under .opencode/toolchains are preferred, system installations are accepted.
"""
from pathlib import Path
import os,subprocess,shutil,hashlib,sys
root=Path(__file__).resolve().parent.parent;src=root/'native/vieneu';build=root/'native/build/vieneu'
env=os.environ.copy();local=root/'.opencode/toolchains'
if (local/'cargo/bin/cargo').is_file():
 env['RUSTUP_HOME']=str(local/'rustup');env['CARGO_HOME']=str(local/'cargo');env['PATH']=str(local/'cargo/bin')+os.pathsep+env['PATH']
cargo=shutil.which('cargo',path=env['PATH'])
if not cargo:sys.exit('Rust is required: see native/vieneu/README.md')
ndk=os.environ.get('ANDROID_NDK_HOME')
if not ndk:
 sdk=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or str(Path.home()/'Library/Android/sdk')
 dirs=sorted((Path(sdk)/'ndk').glob('*'));ndk=str(dirs[-1]) if dirs else ''
pre=next((Path(ndk)/'toolchains/llvm/prebuilt').glob('*'),None)
if not pre:sys.exit('Android NDK toolchain missing')
abis=[('arm64-v8a','aarch64-linux-android','aarch64-linux-android26'),('x86_64','x86_64-linux-android','x86_64-linux-android26'),('armeabi-v7a','armv7-linux-androideabi','armv7a-linux-androideabi26'),('x86','i686-linux-android','i686-linux-android26')]
if '--abi' in sys.argv:abis=[x for x in abis if x[0]==sys.argv[sys.argv.index('--abi')+1]]
assert abis
for abi,target,clangTarget in abis:
 work=build/abi;work.mkdir(parents=True,exist_ok=True);out=root/'android/src/main/jniLibs'/abi;out.mkdir(parents=True,exist_ok=True)
 inputs=list((src/'sea-g2p').rglob('*.rs'))+[Path(__file__),src/'sea-g2p/Cargo.toml',src/'sea-g2p/Cargo.lock',src/'nano.cpp']+list((src/'ort').glob('*.h'))
 stamp=hashlib.sha256(b''.join(p.read_bytes() for p in sorted(inputs))).hexdigest()
 if (work/'verified-build').is_file() and (work/'verified-build').read_text()==stamp and all((out/n).is_file() for n in ['libsea_g2p_rs.so','libvieneu-nano-jni.so']):continue
 e=env.copy();e['CARGO_TARGET_'+target.upper().replace('-','_')+'_LINKER']=str(pre/'bin'/f'{clangTarget}-clang');e['CARGO_TARGET_DIR']=str(work/'rust');e['RUSTFLAGS']='-C link-arg=-Wl,-z,max-page-size=16384'
 subprocess.run([cargo,'build','--manifest-path',str(src/'sea-g2p/Cargo.toml'),'--release','--locked','--no-default-features','--features','capi','--target',target],env=e,check=True)
 shutil.copy2(work/'rust'/target/'release/libsea_g2p_rs.so',out/'libsea_g2p_rs.so')
 subprocess.run([str(pre/'bin'/f'{clangTarget}-clang++'),'-std=c++17','-O2','-fPIC','-shared','-static-libstdc++','-Wl,-z,max-page-size=16384','-I'+str(src/'ort'),str(src/'nano.cpp'),'-L'+str(out),'-lsea_g2p_rs','-ldl','-o',str(out/'libvieneu-nano-jni.so')],check=True)
 (work/'verified-build').write_text(stamp)
 print('Built VieNeu native:',abi,flush=True)
