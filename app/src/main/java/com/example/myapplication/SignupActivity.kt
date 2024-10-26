package com.example.myapplication
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException

class SignupActivity : AppCompatActivity() {

    private lateinit var emailet: EditText
    private lateinit var passwordet: EditText
    private lateinit var nameet: EditText // EditText for name
    private lateinit var repasswordet: EditText // EditText for re-entering password
    private lateinit var btn: Button
    private lateinit var googleBtn: LinearLayout
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val TAG = "SignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Bind UI elements
        emailet = findViewById(R.id.email)
        passwordet = findViewById(R.id.password)
        nameet = findViewById(R.id.name)
        repasswordet = findViewById(R.id.repassword)
        btn = findViewById(R.id.signup_btn)
        googleBtn = findViewById(R.id.signup_google)

        // Set up Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Sign up button logic
        btn.setOnClickListener {
            val email = emailet.text.toString().trim()
            val password = passwordet.text.toString().trim()
            val name = nameet.text.toString().trim()
            val repassword = repasswordet.text.toString().trim()

            // Validation checks
            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || repassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != repassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user with email and password
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // User registration successful
                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()

                        // Get the current user
                        val user: FirebaseUser? = mAuth.currentUser
                        user?.let {
                            saveUserData(it.uid, name, email)
                        }

                        // Navigate to the next activity
                        val intent = Intent(this, Cammodule::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        // Google Sign-In button logic
        googleBtn.setOnClickListener {
            signInWithGoogle()
        }
    }

    // Save user data to Firestore
    private fun saveUserData(userId: String, name: String, email: String) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to email
        )

        firestore.collection("users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Log.d(TAG, "User data successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error writing user data", e)
            }
    }

    // Google Sign-In
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, 1000)
    }

    // Handle Google Sign-In result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                if (account != null) {
                    navigateToSecondActivity()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Something went wrong: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Google sign-in failed", e)
            }
        }
    }

    // Navigate to the next activity
    private fun navigateToSecondActivity() {
        val intent = Intent(this, Cammodule::class.java)
        startActivity(intent)
        finish()
    }
}
