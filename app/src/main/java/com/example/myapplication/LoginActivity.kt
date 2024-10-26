package com.example.myapplication
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var emailet: EditText
    private lateinit var passwordet: EditText
    private lateinit var btn: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Bind UI elements
        emailet = findViewById(R.id.email_login)
        passwordet = findViewById(R.id.password_login)
        btn = findViewById(R.id.login_button_login)

        // Set click listener for login button
        btn.setOnClickListener {
            val email = emailet.text.toString().trim()
            val password = passwordet.text.toString().trim()

            // Perform login using Firebase Authentication
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login successful
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                        // Navigate to cammodule activity
                        val intent = Intent(this, Cammodule::class.java)
                        startActivity(intent)
                        finish()  // Close the current activity
                    } else {
                        // Login failed
                        Toast.makeText(
                            this,
                            "Authentication failed. Please check your credentials.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

    }
}
