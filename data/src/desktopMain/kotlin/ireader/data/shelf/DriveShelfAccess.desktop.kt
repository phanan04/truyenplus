package ireader.data.shelf
import ireader.domain.models.shelf.*
actual fun createShelfAccess():ShelfAccess=object:ShelfAccess {
 override suspend fun token():String=throw ShelfException(ShelfFailure.Unsupported)
 override fun invalidate(){}
}
