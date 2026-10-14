package ireader.domain.usecases.local

import ireader.domain.data.repository.BookRepository
import ireader.domain.usecases.local.book_usecases.ToggleBookPinUseCase
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BookPinCancellationTest {
    @Test fun cancellingPinDoesNotBecomeASilentSuccessfulBatch()=runTest {
        val repo=mockk<BookRepository>()
        coEvery {repo.getMaxPinnedOrder()} returns 3
        coEvery {repo.updatePinStatus(7,true,4)} throws CancellationException("cancelled")
        assertFailsWith<CancellationException> {ToggleBookPinUseCase(repo).togglePin(7,true)}
    }
}
