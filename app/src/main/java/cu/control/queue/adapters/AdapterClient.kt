package cu.control.queue.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.turingtechnologies.materialscrollbar.ICustomAdapter
import cu.control.queue.R
import cu.control.queue.adapters.viewHolders.ViewHolderClient
import cu.control.queue.repository.AppDataBase
import cu.control.queue.repository.entitys.Client
import cu.control.queue.utils.Conts.Companion.formatDateOnlyTime
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

class AdapterClient : RecyclerView.Adapter<ViewHolderClient>(), ICustomAdapter {

    var contentList: List<Client> = ArrayList()
    var checkMode = true
    var done: Boolean = false
    var queueId: Long = 0
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClient {
        context = parent.context
        return ViewHolderClient(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_client,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return contentList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolderClient, position: Int) {
        val client = contentList[position]

        holder.layoutBackground.background =
            ContextCompat.getDrawable(
                holder.layoutBackground.context,
                when {
                    client.searched!! -> R.drawable.item_accent_bg
                    client.selected!! && done -> {
                        R.drawable.item_green_bg
                    }
                    client.selected!! && !done -> {
                        R.drawable.item_red_bg
                    }
                    checkMode && position % 2 != 0 -> R.drawable.item_blue_bg
                    checkMode && position % 2 == 0 -> R.drawable.bg_item_dark_blue
                    position % 2 != 0 -> R.drawable.item_white_bg
                    else -> R.drawable.bg_item_dark
                }
            )


        when {
            !client.isChecked -> {
                holder.clientNumber.visibility = View.VISIBLE
                holder.imageViewCheck.visibility = View.GONE
                holder.imageView.visibility = View.VISIBLE
                holder.imageView.background = ContextCompat.getDrawable(
                    holder.imageView.context,
                    R.drawable.round_accent_bg
                )
                holder.imageView.setImageDrawable(null)

                holder.clientNumber.text = client.number.toString()
            }
            client.isChecked -> {
                holder.clientNumber.visibility = View.GONE
                holder.imageView.visibility = View.GONE
                holder.imageViewCheck.visibility = View.VISIBLE
            }
        }

        holder.textViewName.text = client.name
        holder.textViewID.text = client.ci
        holder.textViewDate.text = formatDateOnlyTime.format(client.lastRegistry)
        holder.textViewReIntents.visibility = if (client.reIntent > 0) {
            holder.textViewReIntents.text = if (client.reIntent > 9) {
                "+9"
            } else {
                client.reIntent.toString()
            }
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.layoutBackground.setOnLongClickListener {
            showPopup(it, client)
            return@setOnLongClickListener true
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showPopup(view: View, client: Client) {

        val context = view.context
        val dao = AppDataBase.getInstance(context).dao()
        val popupMenu = PopupMenu(context, view)
        (context as Activity).menuInflater.inflate(R.menu.menu_client, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    AlertDialog.Builder(context)
                        .setTitle("Eliminar")
                        .setMessage("¿Desea eliminar a " + client.name + " de la lista?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar") { _, _ ->
                            Completable.create {
                                val size = contentList.size - 1
                                dao.deleteClientFromQueue(client.id, queueId)
                                dao.updateQueueSize(queueId, size)
                                it.onComplete()
                            }
                                .observeOn(Schedulers.io())
                                .subscribeOn(Schedulers.io())
                                .subscribe().addTo(CompositeDisposable())
                        }
                        .create()
                        .show()
                }
                R.id.action_check -> {
                    val message =
                        "Desea actualizar a ${client.name} como ${if (client.isChecked) "no chequeado" else "chequeado"}?"
                    AlertDialog.Builder(context)
                        .setMessage(message)
                        .setPositiveButton("Actualizar") { _, _ ->
                            Completable.create { emitter ->
                                val clientInQueue = dao.getClientFromQueue(client.id, queueId)
                                clientInQueue?.isChecked = !client.isChecked
                                dao.insertClientInQueue(clientInQueue!!)
                                emitter.onComplete()
                            }.observeOn(Schedulers.io())
                                .subscribeOn(Schedulers.io())
                                .subscribe()
                        }
                        .create().show()
                }
            }
            false
        }
        val wrapper = ContextThemeWrapper(context, R.style.PopupWhite)
        val menuPopupHelper =
            MenuPopupHelper(wrapper, popupMenu.menu as MenuBuilder, view)
        menuPopupHelper.setForceShowIcon(true)
        menuPopupHelper.show()
    }

    override fun getCustomStringForElement(element: Int): String {
        return if (contentList.isEmpty())
            ""
        else
            contentList[element].number.toString()
    }
}