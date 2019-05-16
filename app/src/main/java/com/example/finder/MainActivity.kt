package com.example.finder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileChooser
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.FilenameFilter


class MainActivity : AppCompatActivity()
{
    val myHandler = Handler()
    var mainPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId)
        {
            R.id.navigation_folder -> {
                val mainLayout: LinearLayout = findViewById(R.id.ln)
                mainLayout.removeAllViews()
                getPermissions()

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    fun getPermissions()
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
        }
        else
        {
            startFolderPicker()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        when(requestCode)
        {
            1001 ->
            {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this@MainActivity, "Permission granted", Toast.LENGTH_SHORT).show()
                    startFolderPicker()
                }
                else
                {
                    Toast.makeText(this@MainActivity, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startFolderPicker()
    {
        val i2 = Intent(applicationContext, FileChooser::class.java)
        i2.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION)
        startActivityForResult(i2, 11)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        if (requestCode == 11 && data != null)
        {
            if (resultCode == Activity.RESULT_OK)
            {

                val file= File(data.data.path)
                val dir = file.isDirectory()

                if(dir)
                {
                    mainPath = data.data.path
                    val t = Thread(Runnable {
                        startProcessing()
                    })
                    t.start()
                }
                else
                {
                    Toast.makeText(this@MainActivity, "Not a directory", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun buildView(imgs: MutableList<String>): LinearLayout
    {
        val layout = createContainer(0, 0, 0, 20, false)

        val textview = createTextBox("Group")
        layout.addView(textview)
        val mainLayout: LinearLayout = findViewById(R.id.ln)
        mainLayout.addView(layout)

        var cnt: Int
        if(imgs.size % 3 == 0)
        {
            var pointer = 0
            cnt = imgs.size / 3

            for(i in 0 until cnt)
            {
                val arr: Array<String> = arrayOf(imgs[pointer], imgs[pointer + 1], imgs[pointer + 2])
                pointer += 3
                layout.addView(createImgLayout(arr,3))
            }
        }
        else
        {
            var pointer = 0
            cnt = imgs.size / 3

            for(i in 0 until cnt)
            {
                val arr: Array<String> = arrayOf(imgs[pointer], imgs[pointer + 1], imgs[pointer + 2])
                pointer += 3
                layout.addView(createImgLayout(arr,3))
            }

            cnt = imgs.size % 3

            if(cnt == 2)
            {
                val arr: Array<String> = arrayOf(imgs[imgs.size - 1], imgs[imgs.size - 2])
                layout.addView(createImgLayout(arr, 3))
            }

            if(cnt == 1)
            {
                val arr: Array<String> = arrayOf(imgs[imgs.size - 1])
                layout.addView(createImgLayout(arr, 3))
            }
        }


        return layout
    }

    fun findSimilar(paths: MutableList<String>): MutableList<MutableList<Img>>
    {
        val match: MutableList<MutableList<Img>> = mutableListOf()
        val imgs: MutableList<Img> = mutableListOf()
        val algo = Algo()
        var i = 0

        for(path in paths)
        {
            val img = Img()
            img.path = path
            imgs.add(img)
        }

        algo.calculateFingerPrint(imgs)

        while(i < imgs.size)
        {
            val temp: MutableList<Img> = mutableListOf()
            val img = imgs.get(i)
            var j = i + 1
            temp.add(img)

            while (j < imgs.size)
            {
                val currImg: Img = imgs.get(j)

                val dist: Int = algo.hamDist(img.hash, currImg.hash)

                if(dist < 4)
                {
                    temp.add(currImg)
                    imgs.remove(currImg)
                    j--
                }

                j++
            }

            match.add(temp)
            i++
        }

        return match
    }

    fun getFile(path: String): MutableList<String>
    {
        val paths: MutableList<String> = mutableListOf()
        val folder = File(path)

        val files = folder.list(object : FilenameFilter
        {
            override fun accept(folder: File, name: String): Boolean
            {
                return name.endsWith("jpeg") || name.endsWith("jpg") || name.endsWith("png")
            }
        })

        for (fileName in files)
        {
            paths.add("$path/$fileName")
        }

        return paths
    }

    fun createList(imgs: MutableList<Img>): MutableList<String>
    {
        val ret: MutableList<String> = mutableListOf()

        for(img in imgs)
        {
            ret.add(img.path)
        }

        return ret
    }

    fun startProcessing()
    {
        if(mainPath == "")
        {
            Toast.makeText(this@MainActivity, "First select folder", Toast.LENGTH_SHORT).show()
            return
        }
        myHandler.post(Runnable{
            var ln: LinearLayout = findViewById(R.id.ln)
            ln.addView(createTextBox("Loading..."))
        })
        val match: MutableList<MutableList<Img>> = findSimilar(getFile(mainPath))
        myHandler.post(Runnable
        {
            var ln: LinearLayout = findViewById(R.id.ln)
            ln.removeAllViews()
            for(imgs in match)
                buildView(createList(imgs))
        })
    }

    fun createImgLayout(path: Array<String>, cnt: Int): LinearLayout
    {
        val cnt1: Int = path.size
        val container = createContainer(0, 0, 0, 0, true)
        val cntarr: MutableList<LinearLayout> = mutableListOf()

        for(i in 0 until cnt)
        {
            val imgContainer = createPhotoContainer()
            cntarr.add(imgContainer)
        }

        for(i in 0 until cnt1)
        {
            val img = createImgBox(path[i])
            cntarr[i].addView(img)
        }

        for(i in 0 until cnt)
        {
            container.addView(cntarr[i])
        }

        return container
    }

    fun createImgBox(path: String): ImageView
    {
        val img = ImageView(this@MainActivity)
        val layoutparms: LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layoutparms.setMargins(10, 10, 10, 10)
        img.layoutParams = layoutparms
        Glide.with(this@MainActivity).load(path).crossFade().diskCacheStrategy(DiskCacheStrategy.ALL).thumbnail(0.5f).into(img)
        img.scaleType = ImageView.ScaleType.CENTER_CROP

        return img
    }

    fun createPhotoContainer(): LinearLayout
    {
        val layout = LinearLayout(this@MainActivity)
        val layoutparms: LinearLayout.LayoutParams = LinearLayout.LayoutParams(400, 400)
        layoutparms.weight = 100.toFloat()
        layout.setGravity(Gravity.CENTER)
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams = layoutparms

        return layout
    }

    fun createContainer(l: Int, r: Int, t: Int, b : Int, isHorizontal: Boolean): LinearLayout
    {
        val layout = LinearLayout(this@MainActivity)
        val layoutparms: LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutparms.setMargins(l, t, r, b)
        layout.layoutParams = layoutparms
        layout.orientation = LinearLayout.VERTICAL
        if(isHorizontal)
            layout.orientation = LinearLayout.HORIZONTAL

        return layout
    }

    fun createTextBox(message: String): TextView
    {
        val textV = TextView(this@MainActivity)
        val layoutparms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutparms.setMargins(10,10,10,10)
        textV.layoutParams = layoutparms
        textV.text = message
        textV.setTextSize(22.toFloat())

        return textV
    }
}