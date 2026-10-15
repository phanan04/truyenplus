package org.ireader.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import ireader.presentation.ui.truyenplus.*
import ireader.presentation.ui.core.theme.themes.*
import org.junit.*
import java.io.File

/** Native Android material comparison, not a claim about physical-device frame rate. */
class ReaderGlassComparisonTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Test fun captureComparison() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug.qa"))
        val out=File(context.getExternalFilesDir(null),"glass-comparison").apply {mkdirs()}
        val phase=mutableIntStateOf(0)
        val shift=mutableFloatStateOf(0f)
        ui.activity.runOnUiThread {ui.activity.setContent {
            val dark=phase.intValue==1
            MaterialTheme(colorScheme=if(dark)TruyenPlusDarkColors else TruyenPlusLightColors) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp)) {
                        Text(if(dark)"BLUR CŨ · TỐI" else "BLUR CŨ · SÁNG",Modifier.weight(1f),fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                        Text("HAZE 2 · KÍNH ĐỀ XUẤT",Modifier.weight(1f),fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                    }
                    Row(Modifier.weight(1f)) {
                        repeat(2) {side->
                            Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal=6.dp)) {
                                MaterialPane(shift,dark,legacy=side==0)
                            }
                        }
                    }
                }
                }
            }
        }}
        for(mode in 0..1) {
            ui.runOnIdle {phase.intValue=mode}
            for(frame in 0 until 36) {
                ui.runOnIdle {shift.floatValue=frame*8f}
                ui.waitForIdle()
                val bitmap=ui.onRoot().captureToImage().asAndroidBitmap()
                File(out,"${mode}-${frame.toString().padStart(3,'0')}.png").outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
                }
            }
        }
        Assert.assertEquals(72,out.listFiles {file->file.extension=="png"}!!.size)
    }

    @Composable private fun MaterialPane(shift:MutableFloatState,dark:Boolean,legacy:Boolean) {
        val colors=MaterialTheme.colorScheme
        val backdrop=remember {ReaderNavigationBackdrop()}
        val layer=rememberGraphicsLayer()
        var sourcePosition by remember {mutableStateOf(Offset.Zero)}
        val source=if(!legacy)Modifier.readerNavigationSource(backdrop) else
            Modifier.onGloballyPositioned {sourcePosition=it.positionInRoot()}.drawWithContent {
                layer.record {drawRect(colors.background);this@drawWithContent.drawContent()}
                drawLayer(layer)
            }
        @Composable fun glass(radius:Int,panel:Boolean=false):Modifier {
            val shape=RoundedCornerShape(radius.dp)
            val base=Modifier.clip(shape)
            if(!legacy)return base.readerNavigationGlass(colors.surface,radius,
                if(panel)ReaderGlassMaterial.Frosted else ReaderGlassMaterial.Compact)
            val sampled=rememberGraphicsLayer()
            val density=LocalDensity.current
            val blur=remember(density) {with(density) {BlurEffect(10.dp.toPx(),10.dp.toPx(),TileMode.Clamp)}}
            var position by remember {mutableStateOf(Offset.Zero)}
            return base.onGloballyPositioned {position=it.positionInRoot()}.drawWithContent {
                drawRect(colors.surface)
                val padding=12.dp.roundToPx();val offset=position-sourcePosition
                sampled.renderEffect=blur
                sampled.record(size=IntSize(size.width.toInt()+padding*2,size.height.toInt()+padding*2)) {
                    translate(padding-offset.x,padding-offset.y) {drawLayer(layer)}
                }
                translate(-padding.toFloat(),-padding.toFloat()) {drawLayer(sampled)}
                drawRect(colors.surface.copy(alpha=if(panel).78f else .72f))
                drawContent()
            }
        }
        CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().clipToBounds().then(source)) {
                    drawRect(colors.background)
                    // Covers/text-like bands move underneath fixed surfaces at the same speed.
                    translate(top=-shift.floatValue.dp.toPx()) {
                        val width=size.width
                        val palette=listOf(Color(0xFFCA8468),Color(0xFF738CAA),Color(0xFFC3A25E),Color(0xFF899E7D))
                        for(row in 0..6) {
                            val top=36.dp.toPx()+row*210.dp.toPx()
                            for(col in 0..2) {
                                val x=10.dp.toPx()+col*(width/3)
                                drawRoundRect(Brush.linearGradient(listOf(palette[(col+row)%4],palette[(col+row+1)%4])),
                                    Offset(x,top),androidx.compose.ui.geometry.Size(width/3-18.dp.toPx(),124.dp.toPx()),
                                    androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                                repeat(3) {line->drawRoundRect(colors.onBackground.copy(alpha=if(line==0).6f else .2f),
                                    Offset(x,top+136.dp.toPx()+line*9.dp.toPx()),
                                    androidx.compose.ui.geometry.Size((width/3-22.dp.toPx())*(if(line==2).6f else 1f),3.dp.toPx()),
                                    androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))}
                            }
                        }
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter).padding(8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.fillMaxWidth().then(glass(24,panel=true)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        Text("Tùy chỉnh nghe",fontWeight=FontWeight.SemiBold,fontSize=18.sp)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.Headphones,null,Modifier.size(18.dp));Text("  Giọng hệ thống",fontSize=14.sp)
                        }
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text("Nổi bật câu đang đọc",Modifier.weight(1f),fontSize=13.sp)
                            Box(Modifier.size(34.dp,20.dp).clip(RoundedCornerShape(10.dp)).background(colors.onSurface)) {
                                Box(Modifier.align(Alignment.CenterEnd).padding(2.dp).size(16.dp).clip(RoundedCornerShape(8.dp)).background(colors.surface))
                            }
                        }
                        Text("Hẹn giờ     30 phút",fontSize=13.sp,color=colors.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth().height(64.dp).then(glass(22)).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Vạn Cổ Thần Đế",fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                            Text("Chương 124",fontSize=12.sp,color=colors.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Pause,null,Modifier.size(24.dp))
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.weight(1f).height(64.dp).then(glass(32)).padding(horizontal=8.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {
                            for((icon,label) in listOf(Icons.Default.Home to "Trang chủ",Icons.Default.MenuBook to "Thư viện",Icons.Default.Cloud to "Kho truyện")) {
                                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                    Icon(icon,null,Modifier.size(24.dp));Text(label,fontSize=10.sp)
                                }
                            }
                        }
                        Box(Modifier.size(64.dp).then(glass(32)),contentAlignment=Alignment.Center) {
                            Icon(Icons.Default.Search,null,Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
