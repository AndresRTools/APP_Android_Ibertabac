// Autor: Edwin Andrés Méndez Arcos
// Empresa: RetailTools
// Descripción: Aplicación Android diseñada para gestionar imágenes, que permite enviarlas a un servidor empresarial utilizando protocolos FTP y VPN.
// Fecha de creación: 31/10/2024
// Última modificación:  31/10/2024
//  Versión: 1.0.0

package com.retailtools.cameraapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import org.apache.commons.net.ftp.FTPClient
import androidx.exifinterface.media.ExifInterface
import android.net.NetworkCapabilities
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.RequiresApi
import java.text.Normalizer
import java.util.concurrent.atomic.AtomicInteger


class MainActivity : AppCompatActivity() {
    private lateinit var autoCompleteTextView: AutoCompleteTextView

    private lateinit var captureButton: Button
    private lateinit var recyclerView: RecyclerView
    private val photoList = mutableListOf<String>()
    private lateinit var photoAdapter: PhotoAdapter

    private val cameraRequestCode = 100
    private val maxPhotos = 5 // Limite máximo de fotos permitidas
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    private lateinit var serverIP: String
    private var port: Int = 2121
    private lateinit var password: String
    private lateinit var user: String

    private lateinit var selectedRoute: String

    private lateinit var editEstanco:EditText
    private lateinit var textViewUser: TextView // Declarar el TextView
    private lateinit var sendButtonConnect: Button
    private lateinit var btnDeleteAll: Button
    private lateinit var textViewStatus: TextView
    private lateinit var btnOpenVPN: Button
    private lateinit var imageUriHighRes: Uri  // URI para la imagen de alta resolución
    private var bitmap: Bitmap? = null


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "IberTabac                                          v1.2"


        initializeViews()
        setupRecyclerView()
        setupCameraLauncher()
        setupCaptureButton()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initializeViews() {
        captureButton = findViewById(R.id.captureButton)
        recyclerView = findViewById(R.id.recyclerView)

        btnOpenVPN = findViewById(R.id.btnOpenVPN)
        sendButtonConnect = findViewById(R.id.sendButtonConnect)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)

        textViewStatus = findViewById(R.id.textViewStatus)
        textViewUser = findViewById(R.id.textViewUser)
        editEstanco = findViewById(R.id.editEstanco)
        autoCompleteTextView = findViewById(R.id.editar_texto2)


        // Cargar la IP del servidor desde strings.xml
        serverIP = getString(R.string.server_ip)
        port = getString(R.string.puerto).toInt()
        password = getString(R.string.pwd)
        user = getString((R.string.usuario))

        val textViewUser: TextView = findViewById(R.id.textViewUser)

////////////////////////////////////////////////////////////////////////////////////
            // ACTIVIDAD DE DESPLEGABLE DEL CAMPO RUTA //
        // Crear un arreglo de datos para el AutoCompleteTextView de 1-50
        val items = Array(50) { i -> "Ruta${i + 1}" }.plus("Especial")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        // Asignar el adaptador al AutoCompleteTextView
        autoCompleteTextView.setAdapter(adapter)
        // Forzar la apertura del desplegable al hacer clic
        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }
        //var selectedRoute: String? = null
        // Establecer un OnItemClickListener
        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            // Obtener el valor seleccionado
            selectedRoute = parent.getItemAtPosition(position) as String
            // Aquí puedes hacer lo que necesites con el valor, como mostrarlo en un Toast
            //Toast.makeText(this, "Ruta seleccionada: $selectedRoute", Toast.LENGTH_SHORT).show()
        }
        // Agregar un TextWatcher para limitar la entrada
        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Modificamos el tipo de caracteres que queremos mostrar en el campo de Ruta
                if (s != null && s.length > 8) {
                    // Limitar el texto a los primeros 6 caracteres
                    autoCompleteTextView.setText(s.substring(0, 8))
                }
            }
            override fun afterTextChanged(s: Editable?) {
            }
        })


////////////////////////////////////////////////////////////////////////////////////
                //ACTIVIDAD DEL BOTON ENVIAR //
        // Configuramos el botón de conectar y subir la imagen
        sendButtonConnect.setOnClickListener {
            //val promoter = editTextUser.text.toString()
            val promoter = textViewUser.text.toString()

            if (photoList.isNotEmpty()) {
                //Log.d("ImageUpload", "Imágenes a subir: $photoList")
                uploadImageToFtp(promoter, selectedRoute,serverIP, port, user, password, photoList)

            } else {
                Toast.makeText(this, "Por favor, captura una imagen primero.", Toast.LENGTH_SHORT).show()
                //Log.d("ImageUpload", "photoList está vacío.")
            }

        }
/////////////////////////////////////////////////////////////////////////////////////
                //ACTIVIDAD DEL BOTON BORRAR//
        btnDeleteAll.setOnClickListener{
            editEstanco.text.clear() // Esto limpia el campo de texto de Nombre de estanco
            photoAdapter.removeAllPhotos() // Elimina todas las fotos
            textViewStatus.text = "" //Borramos comentarios
        }
/////////////////////////////////////////////////////////////////////////////////////
                //ACTIVIDAD DEL BOTON ABRIR VNP//
        btnOpenVPN.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                openVPNSettings()
            }
        }

/////////////////////////////////////////////////////////////////////////////////////
                // ACTIVIDAD PARA RECUPERAR Y MOSTRAR UN NOMBRE DE USUARIO(PROMOTOR)
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedUsername = sharedPreferences.getString("username", null)

        // Si hay un nombre de usuario guardado, mostrarlo
        if (savedUsername != null) {
            textViewUser.text = savedUsername // Establecer el nombre de usuario existente
        } else {
            textViewUser.text = getString(R.string.User_message) // Mensaje por defecto
        }

/////////////////////////////////////////////////////////////////////////////////////
                // ACTIVIDAD DEL BOTON DE CONFIGURACION(TRES PUNTOS)
        val menuIcon: ImageButton = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener {
            // Abrir el menú de configuracion
            openMenu()
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////
            //ACTIVIDAD DEL BOTON DE TOMAR FOTOS
    private fun setupCaptureButton() {
        captureButton.setOnClickListener {
            val nameEstanco = editEstanco.text.toString()
            val normalizedText = normalizeText(this, nameEstanco)
            // Comprobamos si el texto está vacío
            if (normalizedText.isNotEmpty()) {
                if (photoList.size >= maxPhotos) {
                    Toast.makeText(this, "Has alcanzado el límite de $maxPhotos fotos", Toast.LENGTH_SHORT).show()
                } else {
                    if (checkCameraPermission()) {
                        openCamera()
                    } else {
                        requestCameraPermission()
                    }
                }
            } else {
                // Si hay caracteres especiales, mostrar el texto original
                Toast.makeText(this, "Texto con caracteres especiales: $nameEstanco", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // SE MUESTRA EL DESPLEGABLE DE LOS TRES PUNTOS
    private fun openMenu() {
        // Crear un PopupMenu
        val popupMenu = PopupMenu(this, findViewById(R.id.menu_icon))
        popupMenu.menuInflater.inflate(R.menu.menu_options, popupMenu.menu)

        // Manejar los clics de los elementos del menú
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {R.id.settings_option -> {
                    // Navegar a la actividad de configuración , podemos añadir mas actividades
                    val intent = Intent(this, PasswordActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
/////////////////////////////////////////////////////////////////////////////////////
    // PERMITE ABRIR EL LISTADO VPN DEL TELEFONO PARA REALIZAR LAS CONEXION Y DESCONEXION
    @RequiresApi(Build.VERSION_CODES.N)
    private fun openVPNSettings() {
        try {
            // Intent para abrir la configuración de VPN
            val intent = Intent(android.provider.Settings.ACTION_VPN_SETTINGS)
            startActivity(intent) // Inicia la actividad de configuración de VPN
        } catch (e: Exception) {
            Toast.makeText(this, "Error al intentar abrir la configuración de VPN", Toast.LENGTH_SHORT).show()
        }
    }
/////////////////////////////////////////////////////////////////////////////////////
    private fun handleImageCapture() {
        // Obtenemos el bitmap correctamente orientado
        val imageBitmap = getCorrectlyOrientedBitmap(imageUriHighRes)
        if (imageBitmap != null) {
            val imageWithDate = addDateToImage(imageBitmap)
            bitmap = imageWithDate

            // Guardamos la imagen en el almacenamiento externo y obtener su ruta
            val imagePath = saveImage(imageWithDate)
            if (!photoList.contains(imagePath)) {
                photoList.add(imagePath)
                photoAdapter.notifyItemInserted(photoList.size - 1)
            }
        } else {
            Toast.makeText(this, "Error al cargar la image de alta resolution.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun normalizeText(context: Context, text: String): String {
        // Paso 1: Normaliza el texto eliminando caracteres diacríticos
        val normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")

        // Paso 2: Verifica si hay caracteres especiales antes de eliminar
        if (!normalizedText.matches("^[a-zA-Z0-9 ]*$".toRegex())) {
            Toast.makeText(context, "El nombre no admite caracteres especiales", Toast.LENGTH_SHORT).show()
        }
        // Paso 3: Quita caracteres especiales dejando solo letras, números y espacios
        return normalizedText.replace("[^a-zA-Z0-9 ]".toRegex(), "")
    }

    private fun saveImage(bitmap: Bitmap ): String {

        val directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // Asegúrate de obtener el directorio correcto
        val nameEstanco = editEstanco.text.toString()
        val nameEstancoNormalized = normalizeText(this, nameEstanco)

        val fileName = "${nameEstancoNormalized}_${System.currentTimeMillis()}.jpg"
        val file = File(directory, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return file.absolutePath // Devuelve la ruta
    }
//AÑADIMOS TEXTO A LAS IMAGENES
    private fun addDateToImage(bitmap: Bitmap): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val nameEstanco = editEstanco.text.toString()
        val nameEstancoNormalized  = normalizeText(this, nameEstanco)

        // Configurar el estilo del texto
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 100f
            isAntiAlias = true
            setShadowLayer(5f, 1f, 1f, Color.BLACK)
        }

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Calcular posiciones para el texto (Nombre ruta)
        val textWidth3 = paint.measureText(selectedRoute)
        val xPosRuta = mutableBitmap.width - textWidth3 - 20f
        val yPosRuta = mutableBitmap.height - 350f
        // Calcular posiciones para el texto (Nombre de estanco)
        val textWidth2 = paint.measureText(nameEstancoNormalized)
        val xPosAdditional = mutableBitmap.width - textWidth2 - 20f
        val yPosAdditional = mutableBitmap.height - 200f
        // Calcular posiciones para el texto (Hora de la foto)
        val textWidth = paint.measureText(currentTime)
        val xPos = mutableBitmap.width - textWidth - 20f
        val yPos = mutableBitmap.height - 50f

        canvas.drawText(selectedRoute, xPosRuta, yPosRuta, paint)         // Dibujar el texto ruta
        canvas.drawText(nameEstancoNormalized, xPosAdditional, yPosAdditional, paint) // Dibujar el texto estanco
        canvas.drawText(currentTime, xPos, yPos, paint)         // Dibujar la fecha y hora

        return mutableBitmap
    }
//ACTIVIDAD QUE LANZA LA CAMARA Y GUARDA LA IMAGEN EN UN ARCHIVO TEMPORAL DE ALTA RESOLUCION
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Creamos un archivo temporal para almacenar la imagen
        val photoFile = createImageFile()
        if (photoFile != null) {
            imageUriHighRes = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriHighRes) // Pasar el URI
            takePictureLauncher.launch(intent) // Lanzar la cámara
        }
    }

//CREAMOS UN ARCHIVO TEMPORAL PARA ALMANENAR EXTERNAMENTE
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return try {
            val file = File(storageDir, "IMG_$timeStamp.jpg")
            if (file.createNewFile()) {
                file // Retorna el archivo creado
            } else {
                null // En caso de que no se pueda crear el archivo
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null // Manejar la excepción
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////
    //ACTIVIDAD REALIACIONADA A LA CONEXION FTP PARA TRASMISION DE DATOS
@SuppressLint("SetTextI18n")
@RequiresApi(Build.VERSION_CODES.M)
private fun uploadImageToFtp(
    promoter: String,
    selectedRoute: String,
    server: String,
    port: Int,
    user: String,
    password: String,
    photoPaths: List<String>
) {
    if (!isVpnConnected()) {
        textViewStatus.text = getString(R.string.statusVPN)
        textViewStatus.setTextColor(Color.RED)
        return
    }

    val ftpClient = FTPClient()
    val totalPhotos = photoPaths.size
    val uploadedCount = AtomicInteger(0)  // Contador para las fotos subidas

    Thread {
        try {
            // Conectar al servidor
            ftpClient.connect(server, port)
            val login = ftpClient.login(user, password)
            if (login) {
                runOnUiThread {
                    textViewStatus.text = getString(R.string.statusConnect, server)
                    textViewStatus.setTextColor(Color.GRAY)
                }

                // Configurar el modo de transferencia pasiva y binaria
                ftpClient.enterLocalPassiveMode()
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

                // Crear el directorio en el servidor
                val directoryName = promoter
                if (!ftpClient.changeWorkingDirectory(directoryName)) {
                    val dirCreated = ftpClient.makeDirectory(directoryName)
                    if (dirCreated) {
                        ftpClient.changeWorkingDirectory(directoryName)
                        runOnUiThread {
                            textViewStatus.text = getString(R.string.createDirectory, directoryName)
                        }
                    } else {
                        runOnUiThread {
                            textViewStatus.text = getString(R.string.errorDirectory, directoryName)
                        }
                        return@Thread
                    }
                } else {
                    runOnUiThread {
                        textViewStatus.text = getString(R.string.existDirectory, directoryName)
                    }
                }

                // Crear subdirectorios con fecha
                val subDirectoryName = createSubDirectories(ftpClient, selectedRoute)

                // Cambiar al subdirectorio creado para subir imágenes
                if (subDirectoryName != null) {
                    ftpClient.changeWorkingDirectory(subDirectoryName)

                    // Subir cada imagen
                    photoPaths.forEach { photoPath ->
                        val file = File(photoPath)
                        val inputStream = FileInputStream(file)

                        // Subir el archivo al servidor FTP
                        val uploadSuccess = ftpClient.storeFile(file.name, inputStream)
                        inputStream.close()

                        if (uploadSuccess) {
                            runOnUiThread {
                                textViewStatus.text = getString(R.string.imageSuccess, file.name)
                            }
                        } else {
                            runOnUiThread {
                                textViewStatus.text = getString(R.string.imageError, file.name)
                            }
                        }

                        // Aumentar el contador de fotos subidas
                        uploadedCount.incrementAndGet()

                        // Comprobar si ya se subieron todas las imágenes
                        if (uploadedCount.get() == totalPhotos) {
                            runOnUiThread {
                                textViewStatus.text = getString(R.string.uploadComplete)
                                // Aquí puedes habilitar otras acciones, como botones o cambiar el estado de la UI
                            }
                        }
                    }
                }
            } else {
                runOnUiThread {
                    textViewStatus.text = getString(R.string.errorAuthentication)
                }
            }
        } catch (ex: IOException) {
            runOnUiThread {
                textViewStatus.text = "Error: ${ex.message}"
            }
        } finally {
            try {
                ftpClient.logout()
                ftpClient.disconnect()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }.start()
}

////////////////////////////////////////////////////////////////////////////////////////////////////
    //ACTIVIDAD PARA DETECTAR CONEXION O DESCONEXION DE VPN
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isVpnConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
////////////////////////////////////////////////////////////////////////////////////////////////////
    //ACTIVIDAD PARA LA CREACION Y GESTION DE LOS DIRECTORIOS
    private fun createSubDirectories(ftpClient: FTPClient, selectedRoute: String): String? {

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())  // Obtener la fecha actual en el formato deseado
        val currentDate = dateFormat.format(Date())                                // Cambia el formato según lo necesites
        val subDirectoryName = "${selectedRoute}_$currentDate"                     // Crear nombre de directorio Ejemplo: "Ruta_20241023"

        // Crear el subdirectorio dentro del directorio principal
        if (ftpClient.changeWorkingDirectory(subDirectoryName)) {
            runOnUiThread {
                textViewStatus.text = getString(R.string.subdirectoryExists, subDirectoryName)
            }
            return subDirectoryName // Retornamos el nombre del subdirectorio existente
        } else {
            val subDirCreated = ftpClient.makeDirectory(subDirectoryName)
            if (subDirCreated) {
                ftpClient.changeWorkingDirectory(subDirectoryName)
                runOnUiThread {
                    textViewStatus.text = getString(R.string.subdirectoryCreate, subDirectoryName)
                }
                return subDirectoryName // Retornar el nombre del subdirectorio creado
            } else {
                runOnUiThread {
                    textViewStatus.text = getString(R.string.subdirectoryError, subDirectoryName)
                }
            }
        }
        return null // Retornar null si no se pudo crear o cambiar al subdirectorio
    }

    private fun setupRecyclerView() {
        photoAdapter = PhotoAdapter(this, photoList) { photoPath ->
            photoAdapter.removePhoto(photoPath) // Método para eliminar foto del adaptador
            Toast.makeText(this, "Foto eliminada: $photoPath", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = photoAdapter
    }

    private fun setupCameraLauncher() {
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    handleImageCapture()
                } else {
                    Toast.makeText(this, "Captura de imagen cancelada", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            cameraRequestCode
        )
    }

    private fun getCorrectlyOrientedBitmap(imageUri: Uri): Bitmap? {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e("ImageProcessing", "Error al abrir InputStream, es null.")
                return null
            }

            val exifInterface = ExifInterface(inputStream)

            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            // Cerrar el inputStream después de obtener los datos EXIF
            inputStream.close()

            // Volver a abrir el InputStream para obtener el Bitmap
            val imageStream = contentResolver.openInputStream(imageUri)
            if (imageStream == null) {
                Log.e("ImageProcessing", "Error al abrir InputStream para Bitmap, es null.")
                return null
            }

            val bitmap = BitmapFactory.decodeStream(imageStream)
            imageStream.close()

            if (bitmap == null) {
                Log.e("ImageProcessing", "Error al decodificar el bitmap, es null.")
                return null
            }

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipImage(bitmap, true)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipImage(bitmap, false)
                else -> bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("ImageProcessing", "IOException al procesar la imagen: ${e.message}")
        }

        return null
    }

    // Rotar la imagen
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    // VOLTER IMAGEN DE HORIZONTAL A VERTICAL
    private fun flipImage(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix()
        if (horizontal) {
            matrix.preScale(-1f, 1f)
        } else {
            matrix.preScale(1f, -1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
