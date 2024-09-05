package eu.example.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.example.happyplaces.activities.AddHappyPlaceActivity
import eu.example.happyplaces.activities.MainActivity
import eu.example.happyplaces.database.DatabaseHandler
import eu.example.happyplaces.databinding.ItemHappyPlaceBinding
import eu.example.happyplaces.models.HappyPlaceModel

open class HappyPlaceAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
):RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var onClickListener: OnClickListener?= null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            ItemHappyPlaceBinding.inflate(
                LayoutInflater.from(parent.context),parent,false)
            )
    }


    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model= list[position]
        if(holder is MyViewHolder){
            holder.binding?.civPlaceImage?.setImageURI(Uri.parse(model.image))
            holder.binding?.tvTitle?.text = model.title
            holder.binding?.tvDescription?.text = model.description

            holder.itemView.setOnClickListener{
                if(onClickListener!= null){
                    onClickListener!!.onClick(position,model)
                }
            }
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    interface OnClickListener{
        fun onClick(position: Int,model: HappyPlaceModel)
    }

    fun notifyEditItem(activity: Activity,position: Int,requestCode:Int){
        val intent = Intent(context,AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, list[position])
        activity.startActivityForResult(intent,requestCode)
        notifyItemChanged(position)
    }

    fun removeAt(position: Int){
        val dvHandler = DatabaseHandler(context)
        val isDeleted = dvHandler.deleteHappyPlace(list[position])
        if(isDeleted>0){
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private class MyViewHolder(val binding: ItemHappyPlaceBinding?):RecyclerView.ViewHolder(binding!!.root)
}