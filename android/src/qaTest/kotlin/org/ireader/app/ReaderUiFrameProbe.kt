package org.ireader.app

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections

/** QA-only native frame measurements. Never interprets emulator results as phone FPS. */
internal class ReaderUiFrameProbe(private val window:Window,private val name:String):AutoCloseable {
    private val thread=HandlerThread("qa-ui-frames").apply {start()}
    private val rows=Collections.synchronizedList(mutableListOf<LongArray>())
    @Volatile var actionLabel:String="initial"
    private val slow=Collections.synchronizedList(mutableListOf<JSONObject>())
    private val listener=Window.OnFrameMetricsAvailableListener {_,m,_->
        rows.add(longArrayOf(m.getMetric(FrameMetrics.TOTAL_DURATION),m.getMetric(FrameMetrics.GPU_DURATION),
            m.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION),m.getMetric(FrameMetrics.DRAW_DURATION)))
        if(m.getMetric(FrameMetrics.TOTAL_DURATION)>100_000_000)slow.add(JSONObject()
            .put("action",actionLabel).put("totalMs",m.getMetric(FrameMetrics.TOTAL_DURATION)/1_000_000.0)
            .put("gpuMs",m.getMetric(FrameMetrics.GPU_DURATION)/1_000_000.0)
            .put("drawMs",m.getMetric(FrameMetrics.DRAW_DURATION)/1_000_000.0)
            .put("layoutMs",m.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)/1_000_000.0)
            .put("unknownDelayMs",m.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION)/1_000_000.0)
            .put("inputMs",m.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)/1_000_000.0)
            .put("animationMs",m.getMetric(FrameMetrics.ANIMATION_DURATION)/1_000_000.0)
            .put("swapMs",m.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)/1_000_000.0)
            .put("syncMs",m.getMetric(FrameMetrics.SYNC_DURATION)/1_000_000.0)
            .put("commandMs",m.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)/1_000_000.0)
            .put("firstDraw",m.getMetric(FrameMetrics.FIRST_DRAW_FRAME)))
    }
    private val started=android.os.SystemClock.elapsedRealtime()
    private val cpuStarted=android.os.Process.getElapsedCpuTime()
    init {window.addOnFrameMetricsAvailableListener(listener,Handler(thread.looper))}
    fun snapshot():JSONObject {
        val copy=synchronized(rows) {rows.toList()}
        val duration=android.os.SystemClock.elapsedRealtime()-started
        val memory=android.os.Debug.MemoryInfo().also {android.os.Debug.getMemoryInfo(it)}
        fun percentile(column:Int,f:Double):Double {
            val sorted=copy.map {it[column]}.filter {it>=0}.sorted()
            return if(sorted.isEmpty())0.0 else sorted[((sorted.size-1)*f).toInt()]/1_000_000.0
        }
        return JSONObject().put("label",name).put("durationMs",duration).put("frames",copy.size)
            .put("cpuTimeMs",android.os.Process.getElapsedCpuTime()-cpuStarted).put("pssKb",memory.totalPss)
            .put("javaHeapKb",(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024)
            .put("medianMs",percentile(0,.5)).put("p95Ms",percentile(0,.95))
            .put("maxMs",percentile(0,1.0)).put("gpuP95Ms",percentile(1,.95))
            .put("layoutP95Ms",percentile(2,.95)).put("drawP95Ms",percentile(3,.95))
            .put("over32ms",copy.count {it[0]>32_000_000}).put("frozen700ms",copy.count {it[0]>700_000_000})
            .put("slowFrames",JSONArray(synchronized(slow){slow.toList()}))
    }
    override fun close() {
        window.removeOnFrameMetricsAvailableListener(listener)
        thread.quitSafely();thread.join(5000)
    }
    companion object {
        fun save(name:String,measurements:List<JSONObject>,actions:List<JSONObject> = emptyList()) {
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            check(context.packageName.endsWith(".debug.qa"))
            val out=File(context.getExternalFilesDir(null),"qa-performance/$name.json").apply {parentFile!!.mkdirs()}
            out.writeText(JSONObject().put("scope","Android emulator, debug QA; not physical-device certification")
                .put("frames",JSONArray(measurements)).put("actions",JSONArray(actions)).toString(2))
            Log.i("TruyenPlusQA","UI performance $name ${out.readText().replace("\n","")}")
        }
    }
}
