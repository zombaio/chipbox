package net.sigmabeta.chipbox.ui.track

import android.view.View
import net.sigmabeta.chipbox.R
import net.sigmabeta.chipbox.model.domain.Track
import net.sigmabeta.chipbox.ui.BaseArrayAdapter
import net.sigmabeta.chipbox.ui.ItemListView

class TrackListAdapter(view: ItemListView<TrackViewHolder>) : BaseArrayAdapter<Track, TrackViewHolder>(view) {
    var playingTrackId: String? = null
        set (value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getLayoutId() = R.layout.list_item_track

    override fun createViewHolder(view: View): TrackViewHolder {
        return TrackViewHolder(view, this)
    }

    override fun bind(holder: TrackViewHolder, item: Track) {
        holder.bind(item)
    }
}