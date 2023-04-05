package pe.cibertec.appocr

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.codelab.mlkit.R
import pe.cibertec.appocr.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}