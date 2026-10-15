package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.listSaver

/** Secondary surfaces retain the content destination that opened them. */
@Immutable
data class ReaderNavigationState(
    val destination:ReaderDestination=ReaderDestination.Home,
    val search:Boolean=false,
    val settings:Boolean=false,
    val progress:Boolean=false,
    val bookId:Long?=null,
) {
    fun select(tab:ReaderDestination)=if(tab==ReaderDestination.Settings)openSettings() else ReaderNavigationState(tab)
    fun openSearch()=copy(search=true,settings=false,progress=false,bookId=null)
    fun openBook(id:Long)=copy(bookId=id,settings=false,progress=false)
    fun openSettings()=copy(settings=true,progress=false)
    fun back()=when {
        progress->copy(progress=false)
        settings->copy(settings=false)
        bookId!=null->copy(bookId=null)
        search->copy(search=false)
        else->ReaderNavigationState()
    }
    companion object {
        fun fromInitialIndex(index:Int)=if(index==ReaderDestination.Settings.ordinal)ReaderNavigationState(settings=true)
            else ReaderNavigationState(ReaderDestination.entries.getOrElse(index){ReaderDestination.Home})
        val Saver=listSaver<ReaderNavigationState,Any>(
            save={listOf(it.destination.ordinal,it.search,it.settings,it.progress,it.bookId ?: Long.MIN_VALUE)},
            restore={ReaderNavigationState(ReaderDestination.entries[it[0] as Int],it[1] as Boolean,it[2] as Boolean,it[3] as Boolean,(it.getOrNull(4) as? Long)?.takeUnless {id->id==Long.MIN_VALUE})})
    }
}
