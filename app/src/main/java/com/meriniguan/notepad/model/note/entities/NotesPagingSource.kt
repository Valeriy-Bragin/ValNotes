package com.meriniguan.notepad.model.note.entities

import androidx.paging.PagingSource
import androidx.paging.PagingState

typealias NotesPageLoader = suspend (pageSize: Int, pageIndex: Int) -> List<Note>

class NotesPagingSource(
    private val loader: NotesPageLoader
) : PagingSource<Int, Note>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Note> {
        val pageIndex = params.key ?: 0

        return try {
            val notes = loader(params.loadSize, pageIndex)
            LoadResult.Page(
                data = notes,
                prevKey = if (pageIndex == 0) null else pageIndex - 1,
                nextKey = if (notes.size == params.loadSize) pageIndex + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(throwable = e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Note>): Int? {
        // get the most recently accessed index in the tasks list:
        val anchorPosition = state.anchorPosition ?: return null
        // convert item index to page index:
        val page = state.closestPageToPosition(anchorPosition) ?: return null
        // page doesn't have 'currentKey' property, so need to calculate it manually:
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}