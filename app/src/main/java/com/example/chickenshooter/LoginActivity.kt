package com.example.chickenshooter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { Firebase.auth }
    private val prefs by lazy { getSharedPreferences("game", MODE_PRIVATE) }

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etDisplayName: EditText
    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: Button
    private lateinit var btnGuest: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Bật cache offline RTDB (chỉ cần 1 lần)
        try {
            Firebase.database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            e.printStackTrace() // đã bật trước đó thì bỏ qua
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etDisplayName = findViewById(R.id.etDisplayName)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGuest = findViewById(R.id.btnGuest)
        progress = findViewById(R.id.progress)

        btnSignIn.setOnClickListener { signIn() }
        btnSignUp.setOnClickListener { signUp() }
        btnGuest.setOnClickListener { signInAnonymous() }
    }

    private fun loading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        btnSignIn.isEnabled = !show
        btnSignUp.isEnabled = !show
        btnGuest.isEnabled = !show
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun signIn() {
        val email = etEmail.text.toString().trim()
        val pass  = etPassword.text.toString()
        if (email.isEmpty() || pass.length < 6) {
            toast("Nhập email hợp lệ và mật khẩu ≥ 6 ký tự"); return
        }
        loading(true)
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    loading(false)
                    toast(task.exception?.localizedMessage ?: "Đăng nhập thất bại")
                    return@addOnCompleteListener
                }
                ensureUserProfileRTDB {
                    loading(false)
                    goOnlineToStartMenu()
                }
            }
    }

    private fun signUp() {
        val email = etEmail.text.toString().trim()
        val pass  = etPassword.text.toString()
        val name  = etDisplayName.text.toString().ifBlank { "Player" }
        if (email.isEmpty() || pass.length < 6) {
            toast("Nhập email hợp lệ và mật khẩu ≥ 6 ký tự"); return
        }
        loading(true)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { t1 ->
                if (!t1.isSuccessful) {
                    loading(false)
                    toast(t1.exception?.localizedMessage ?: "Đăng ký thất bại")
                    return@addOnCompleteListener
                }
                ensureUserProfileRTDB(name) {
                    loading(false)
                    goOnlineToStartMenu()
                }
            }
    }

    private fun signInAnonymous() {
        loading(true)
        auth.signInAnonymously()
            .addOnCompleteListener { t ->
                if (!t.isSuccessful) {
                    loading(false)
                    toast(t.exception?.localizedMessage ?: "Anonymous thất bại")
                    return@addOnCompleteListener
                }
                ensureUserProfileRTDB {
                    loading(false)
                    goOnlineToStartMenu()
                }
            }
    }

    // Tạo/đảm bảo user profile trong RTDB
    private fun ensureUserProfileRTDB(
        displayName: String? = null,
        onOk: () -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onOk()
        val database = Firebase.database("https://chickenshooter-bd531-default-rtdb.asia-southeast1.firebasedatabase.app")
        val ref = database.getReference("users/$uid")
        val now = System.currentTimeMillis()
        val init = mapOf(
            "displayName" to (displayName ?: "Player"),
            "coins" to 0L,
            "createdAt" to now,
            "updatedAt" to now
        )
        ref.updateChildren(init)
            .addOnSuccessListener { onOk() }
            .addOnFailureListener { e ->
                toast("RTDB lỗi: ${e.message}")
                onOk() // tránh kẹt UI
            }
    }

    private fun goOnlineToStartMenu() {
        prefs.edit().putBoolean("offline_mode", false).apply()
        val intent = Intent(this, StartMenuActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        }
        startActivity(intent)
        finish()
    }
}
