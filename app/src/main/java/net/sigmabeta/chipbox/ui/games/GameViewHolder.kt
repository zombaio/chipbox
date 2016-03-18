package net.sigmabeta.chipbox.ui.games

import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.grid_item_game.view.*
import net.sigmabeta.chipbox.model.domain.Game
import net.sigmabeta.chipbox.ui.BaseViewHolder
import net.sigmabeta.chipbox.util.loadImageLowQuality

class GameViewHolder(view: View, adapter: GameGridAdapter) : BaseViewHolder<Game, GameViewHolder, GameGridAdapter>(view, adapter), View.OnClickListener {
    var gameId: Long? = null

    var boundAtLeastOnce = false

    override fun getId(): Long? {
        return gameId
    }

    override fun bind(toBind: Game) {
        gameId = toBind.id

        view.text_game_title.text = toBind.title
        view.text_company.text = toBind.company

        toBind.artLocal?.let {
            view.image_game_box_art.loadImageLowQuality(it, boundAtLeastOnce, true)
        } ?: let {
            view.image_game_box_art.loadImageLowQuality(Game.PICASSO_ASSET_ALBUM_ART_BLANK, boundAtLeastOnce, true)
        }

        boundAtLeastOnce = true
    }

    fun getSharedImage(): ImageView = view.image_game_box_art
}