package org.ireader.app

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import android.view.Choreographer
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.glass.*
import ireader.presentation.ui.truyenplus.*
import org.junit.*
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Same moving source and surface sizes, measured on the same Android renderer. QA only. */
@OptIn(ExperimentalHazeApi::class)
class ReaderGlassFrameTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Test fun movingBackdropFrameCost() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".debug.qa"))
        val legacy=InstrumentationRegistry.getArguments().getString("legacy")=="true"
        val shift=mutableFloatStateOf(0f)
        ui.activity.runOnUiThread { ui.activity.setContent {
            MaterialTheme {
                val backdrop=remember {ReaderNavigationBackdrop()}
                CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        Canvas(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                            for(i in -1..40) drawRect(if(i%2==0)Color(0xFF734B9B) else Color(0xFF15344C),
                                Offset(0f,i*100f+shift.floatValue),Size(size.width,50f))
                        }
                        @Composable fun surface(radius:Int,panel:Boolean):Modifier {
                            val base=Modifier.clip(RoundedCornerShape(radius.dp))
                            if(!legacy)return base.readerNavigationGlass(Color(0xFF1B1B1D),radius,
                                if(panel)ReaderGlassMaterial.Frosted else ReaderGlassMaterial.Compact)
                            val style=remember {GlassStyle.regular.then {
                                shape(RoundedCornerShape(radius.dp));backgroundColor(Color(0xFF1B1B1D))
                                tint(Color(0xFF1B1B1D).copy(alpha=if(panel).78f else .72f))
                                optics(refractionStrength=.25f,refractionDisplacement=5.dp,blurRadius=16.dp)
                                chromaticAberrationStrength(0f);specularIntensity(.2f);edgeShadow(Color.Transparent)
                            }}
                            return base.drawWithContent {drawRect(Color(0xFF1B1B1D));drawContent()}
                                .hazeGlass(HazeInput.Sources(backdrop.state),style,performanceMode=HazePerformanceMode.Performance)
                        }
                        Column(Modifier.align(Alignment.BottomCenter).padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.fillMaxWidth().height(300.dp).then(surface(24,true)))
                            Box(Modifier.fillMaxWidth().height(64.dp).then(surface(22,false)))
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f).height(64.dp).then(surface(32,false)))
                                Box(Modifier.size(64.dp).then(surface(32,false)))
                            }
                        }
                    }
                }
            }
        }}
        ui.waitForIdle()
        val thread=HandlerThread("qa-frame-metrics").apply {start()}
        val timings=Collections.synchronizedList(mutableListOf<Long>())
        val gpuTimings=Collections.synchronizedList(mutableListOf<Long>())
        val listener=Window.OnFrameMetricsAvailableListener {_,metrics,_->
            timings.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))
            val gpu=metrics.getMetric(FrameMetrics.GPU_DURATION)
            if(gpu>0)gpuTimings.add(gpu)
        }
        fun animateFrames(count:Int) {
            val finished=CountDownLatch(1)
            ui.activity.runOnUiThread {
                val choreographer=Choreographer.getInstance()
                val callback=object:Choreographer.FrameCallback {
                    var frame=0
                    override fun doFrame(time:Long) {
                        shift.floatValue=(frame%40)*2.5f
                        if(++frame<count)choreographer.postFrameCallback(this) else finished.countDown()
                    }
                }
                choreographer.postFrameCallback(callback)
            }
            Assert.assertTrue("Animation timed out",finished.await(45,TimeUnit.SECONDS))
            ui.waitForIdle()
        }
        try {
            animateFrames(60)
            ui.activity.runOnUiThread {ui.activity.window.addOnFrameMetricsAvailableListener(listener,Handler(thread.looper))}
            animateFrames(180)
        } finally {
            ui.activity.runOnUiThread {ui.activity.window.removeOnFrameMetricsAvailableListener(listener)}
            thread.quitSafely();thread.join()
        }
        val values=synchronized(timings) {timings.sorted()}
        Assert.assertTrue("No usable Android frames",values.size>30)
        fun percentile(f:Double)=values[((values.size-1)*f).toInt()]/1_000_000.0
        Log.i("TruyenPlusQA","Glass frames legacy=$legacy count=${values.size} medianMs=${percentile(.5)} p95Ms=${percentile(.95)} over32ms=${values.count {it>32_000_000}}")
        val gpu=synchronized(gpuTimings) {gpuTimings.sorted()}
        if(gpu.isNotEmpty())Log.i("TruyenPlusQA","Glass GPU legacy=$legacy count=${gpu.size} medianMs=${gpu[gpu.size/2]/1_000_000.0} p95Ms=${gpu[((gpu.size-1)*.95).toInt()]/1_000_000.0}")
    }
}
