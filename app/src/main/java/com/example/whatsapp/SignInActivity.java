package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.whatsapp.Models.Users;
import com.example.whatsapp.databinding.ActivitySignInBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class SignInActivity extends AppCompatActivity {
ActivitySignInBinding binding;
ProgressDialog progressDialog;
FirebaseAuth auth;
GoogleSignInClient mGoogleSignInClient;
FirebaseDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


    getSupportActionBar().hide();


        auth=FirebaseAuth.getInstance();

        database=FirebaseDatabase.getInstance();

        progressDialog=new ProgressDialog(SignInActivity.this);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Login to your Account");

       //Configure Google sign in
        GoogleSignInOptions gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail()
                .build();
        mGoogleSignInClient= GoogleSignIn.getClient(this,gso);






        binding.btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.etEmail.getText().toString().isEmpty()){
                    binding.etEmail.setError("Enter your Email");
                    return;

                }
                if (binding.etPassword.getText().toString().isEmpty()){
                    binding.etPassword.setError("Enter password");
                    return;
                }

                progressDialog.show();
                auth.signInWithEmailAndPassword(binding.etEmail.getText().toString(),binding.etPassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if ((task.isSuccessful())){
                            Intent intent=new Intent(SignInActivity.this,MainActivity.class);
                            startActivity(intent);
                        }
                        else {
                            Toast.makeText(SignInActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

        binding.tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SignInActivity.this,SignUpActivity.class);
                startActivity(intent);
            }
        });

        binding.btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });


        if (auth.getCurrentUser()!=null){
            Intent intent=new Intent(SignInActivity.this,MainActivity.class);
            startActivity(intent);
        }

    }
    int RC_SIGN_IN=65;
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void onActivityResult(int requestCode,int resultCode,Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==RC_SIGN_IN){
            Task<GoogleSignInAccount> task =GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                //Google Sign in was successful,Authenticate with firebase
                GoogleSignInAccount account=task.getResult(ApiException.class);
                Log.d("TAG","firebaseAuthWithGoogle:"+account.getIdToken());

                firebaseAuthWithGoogle(account.getIdToken());

            }catch (ApiException e){
                //Google Sign in failed
            Log.w("TAG","Google sign in failed",e);
            }
        }
    }

private  void firebaseAuthWithGoogle(String idToken){
    AuthCredential credential= GoogleAuthProvider.getCredential(idToken,null);
    auth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if ((task.isSuccessful())){
                        //sign in is success,update Ui with signed in user
                        Log.d("TAG","signInWithCredential:success");
                        FirebaseUser user =auth.getCurrentUser();
                        Users users=new Users();
                        users.setUserId(user.getUid());
                        users.setUserName(user.getDisplayName());
                        users.setProfilePicture(user.getPhotoUrl().toString());

                        database.getReference().child("users").child(user.getUid()).setValue(users);


                        Intent intent=new Intent(SignInActivity.this,MainActivity.class);
                        startActivity(intent);
                        Toast.makeText(SignInActivity.this, "Sign in with Google", Toast.LENGTH_SHORT).show();
                    }else{
                        //if sign in fails,display a message to user.

                        Log.w("TAG","signInCredential:failure",task.getException());
                        Toast.makeText(SignInActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Snackbar.make(binding.getRoot(),"Authentication Failed",Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
}





}