package ireader.presentation.ui.truyenplus

import kotlin.test.*

class BooksNavigationArchitectureTest {
    @Test fun contentDestinationsExcludeUtilitiesAndRepositoryIsFirstClass() {
        assertEquals(listOf(ReaderDestination.Home,ReaderDestination.Library,ReaderDestination.Shelf),ReaderAppScope.destinations)
    }
    @Test fun searchAndAccountReturnToTheirOriginInsteadOfReplacingLibrary() {
        val origin=ReaderNavigationState(ReaderDestination.Shelf)
        val search=origin.openSearch()
        assertTrue(search.search)
        assertEquals(ReaderDestination.Shelf,search.destination)
        val account=search.openSettings().copy(progress=true)
        assertTrue(account.back().settings)
        assertFalse(account.back().progress)
        assertEquals(search,account.back().back())
        assertEquals(origin,search.back())
    }
    @Test fun selectingTabClosesSecondarySurfacesAndLegacySettingsStillWorks() {
        assertEquals(ReaderNavigationState(ReaderDestination.Library),ReaderNavigationState().openSearch().openSettings().select(ReaderDestination.Library))
        val legacy=ReaderNavigationState.fromInitialIndex(2)
        assertTrue(legacy.settings)
        assertEquals(ReaderDestination.Home,legacy.back().destination)
        assertEquals(ReaderDestination.Home,ReaderNavigationState(ReaderDestination.Shelf).back().destination)
    }
    @Test fun detailsStayInsideTheirContentContextAndBackReturnsBeforeLeavingSearch() {
        val search=ReaderNavigationState(ReaderDestination.Shelf).openSearch()
        val details=search.openBook(42)
        assertEquals(42L,details.bookId)
        assertEquals(ReaderDestination.Shelf,details.destination)
        assertTrue(details.search)
        assertEquals(search,details.back())
        assertNull(details.select(ReaderDestination.Library).bookId)
        assertNull(details.openSearch().bookId)
    }

}
