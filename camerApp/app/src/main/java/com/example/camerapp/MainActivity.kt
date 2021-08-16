package com.example.camerapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ActionMenuView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.camerapp.databinding.ActivityMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE = 1   //카메라 사진 촬영 요청코드
    lateinit var curPhotoPath: String   //문자열 형태의 사진 경로 값
    lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setPermission() // 권한을 체크하는 기능을 수행

        binding.btnCamera.setOnClickListener {
            takeCapture()   //기본 카메라 앱을 실행하여 사진 촬영.
        }

    }

    private fun takeCapture() {
        // 기본 카메라 앱 실행
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also{
                val photoFile: File? = try {
                    createImageFile()      //이미지 만들기
                } catch (ex: IOException){
                    null
                }
                photoFile?.also{
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.camerapp.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())  //사진이름을 설정할 때 날짜로 설정하게 만들 String
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)     //임시저장공간
        return File.createTempFile("JPEG_${timestamp}",".jpg",storageDir)
            .apply{curPhotoPath = absolutePath} //사진 경로 절대경로로 만듬
    }

    private fun setPermission() {
        val permission = object : PermissionListener {  //singleTon이라 사용하는 것으로 판단
            override fun onPermissionGranted() {    //설정해놓은 위험권한들이 허용 되었을 경우 이곳을 수행.
                Toast.makeText(this@MainActivity,"권한이 허용 되었습니다.",Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {  //설정해놓은 위험권한 둘 중 거부를 한 경우 이곳을 수행
                Toast.makeText(this@MainActivity,"권한이 거부 되었습니다.",Toast.LENGTH_SHORT).show()
            }
        }

        TedPermission.with(this)       //build.gradle의 tedPermission이 넣어있어야 사용가능
            .setPermissionListener(permission)  //
            .setRationaleMessage("카메라 앱을 사용하시려면 권한을 허용해주세요.")   //
            .setDeniedMessage("권한을 거부하셨습니다.")   //권한을 거부했을경우
            .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA)
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {   //startActivityForResult를 통해서 기본 카메라 앱으로부터 받아온 사진 결과 값
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){  //이미지를 성공적으로 가져왔다면 실행
            val bitmap: Bitmap
            val file = File(curPhotoPath)   //현재 사진이 저장된 값
            if(Build.VERSION.SDK_INT < 28){     //안드로이드 9.0 (pie) 버전보다 낮을 경우
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file)) //비트맵 Uri와 연결
                binding.ivProfile.setImageBitmap(bitmap)    //촬영했던 사진이 그대로 이미지뷰에 기록됨
            }else{  //안드로이드 9.0(pie)버전보다 높을 경우
                val decode = ImageDecoder.createSource(     //ImageDecoder을 이용해 파일을 가져옴
                    this.contentResolver,
                    Uri.fromFile(file)
                )
                bitmap = ImageDecoder.decodeBitmap(decode)
                binding.ivProfile.setImageBitmap(bitmap)
            }
            savePhoto(bitmap)
        }
    }

    private fun savePhoto(bitmap: Bitmap) {
        val folderPath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/"  //사진폴더로 저장하기 위한 경로 선언
        val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())  //사진이름을 설정할 때 날짜로 설정하게 만들 String
        val fileName = "${timestamp}.jpeg"
        val folder = File(folderPath)
        if(!folder.isDirectory) { //현재 해당 경로에 폴더가 존재하지 않는다면
            folder.mkdirs() //디렉도리생성 , 유닉스기초배우면 앎
        }
        //실제적인 저장처리
        val out = FileOutputStream(folderPath + fileName)
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,out)
        Toast.makeText(this,"사진이 앨범에 저장되었습니다.",Toast.LENGTH_SHORT).show()
    }
}