package eu.example.happyplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.example.happyplaces.adapters.HappyPlaceAdapter
import eu.example.happyplaces.database.DatabaseHandler
import eu.example.happyplaces.databinding.ActivityMainBinding
import eu.example.happyplaces.models.HappyPlaceModel
import eu.example.happyplaces.utils.SwipeToDeleteCallback
import eu.example.happyplaces.utils.SwipeToEditCallback

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        binding?.fabAddHappyPlace?.setOnClickListener {
            val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent,ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }
        getHappyPlaceListFromLocalDB()
    }

    private fun getHappyPlaceListFromLocalDB(){
        val dbHandler = DatabaseHandler(this)
        val getHappyPlaceList : ArrayList<HappyPlaceModel> = dbHandler.getHappyPlaceList()

        if(getHappyPlaceList.isNotEmpty()){
            binding?.rvHappyPlacesList?.visibility =View.VISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.GONE
            setupHappyPlacesRecycleView(getHappyPlaceList)
        }else{
            binding?.rvHappyPlacesList?.visibility =View.GONE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }
    }

    private fun setupHappyPlacesRecycleView(
        happyPlaceList:ArrayList<HappyPlaceModel>){
        binding?.rvHappyPlacesList?.layoutManager =
            LinearLayoutManager(this)
        binding?.rvHappyPlacesList?.setHasFixedSize(true)


        val placesAdapter = HappyPlaceAdapter(this,happyPlaceList)
        binding?.rvHappyPlacesList?.adapter = placesAdapter

        placesAdapter.setOnClickListener(object : HappyPlaceAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity,HappyPlaceDetailActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS,model)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object : SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvHappyPlacesList?.adapter as HappyPlaceAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition,
                    ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }

        val editItemTouchHelper  = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)

        val deleteSwipeHandler = object : SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding?.rvHappyPlacesList?.adapter as HappyPlaceAdapter
                adapter.removeAt(viewHolder.adapterPosition)

                getHappyPlaceListFromLocalDB()
            }
        }

        val deleteItemTouchHelper  = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE && resultCode ==Activity.RESULT_OK)
            getHappyPlaceListFromLocalDB()

        else Log.e("Activity", "Cancelled or Back pressed")

    }

    companion object{
        var ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        var EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}