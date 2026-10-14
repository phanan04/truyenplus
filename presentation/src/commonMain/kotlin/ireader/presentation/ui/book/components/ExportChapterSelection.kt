package ireader.presentation.ui.book.components

import androidx.compose.runtime.saveable.listSaver

data class ExportChapterSelection(val all:Boolean,val ids:Set<Long>) {
    fun toggle(id:Long,checked:Boolean)=copy(all=false,ids=if(checked)ids+id else ids-id)
    fun reconcile(available:Set<Long>)=copy(ids=if(all)available else ids.intersect(available))
}
val ExportChapterSelectionSaver=listSaver<ExportChapterSelection,Any>(
    save={listOf(it.all)+it.ids.toList()},
    restore={values->ExportChapterSelection(values.first() as Boolean,values.drop(1).map {(it as Number).toLong()}.toSet())})
