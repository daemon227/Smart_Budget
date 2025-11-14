package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class home_screen extends AppCompatActivity {
    private EditText mEmail;
    private EditText mPass;
    private CheckBox remember;
    private Button btnLogin;
    private TextView mforget_password;
    private TextView mSignUpHere;

    private ProgressDialog mDialog;
    private FirebaseAuth mAuth;
    
    // Lưu references để remove listeners
    private CompoundButton.OnCheckedChangeListener rememberListener;
    private View.OnClickListener signUpListener;
    private View.OnClickListener forgetPasswordListener;
    private View.OnClickListener loginListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        mAuth=FirebaseAuth.getInstance();
        mDialog=new ProgressDialog(this);

        loginDetails();
    }

    private void loginDetails() {
        mEmail = findViewById(R.id.email_login);
        mPass = findViewById(R.id.password_login);
        btnLogin = findViewById(R.id.btn_login);
        mforget_password = findViewById(R.id.forgot_password);
        mSignUpHere = findViewById(R.id.signup_reg);
        remember = findViewById(R.id.checkBox2);

        SharedPreferences preferences=getSharedPreferences("checkbox",MODE_PRIVATE);
        String checkbox=preferences.getString("remember","");
        if(checkbox.equals("true"))
        {
            Intent intent=new Intent(home_screen.this,first_home_page.class);
            startActivity(intent);
            finish();
        }
        else if(!checkbox.equals("false"))
        {

        }

        // Lưu listener reference để có thể remove sau
        rememberListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.isChecked())
                {
                    SharedPreferences preferences=getSharedPreferences("checkbox",MODE_PRIVATE);
                    SharedPreferences.Editor editor=preferences.edit();
                    editor.putString("remember","true");
                    editor.apply();
                    Toast.makeText(home_screen.this,"Ghi nhớ mật khẩu..",Toast.LENGTH_SHORT).show();
                }
                else if(!buttonView.isChecked())
                {
                    SharedPreferences preferences=getSharedPreferences("checkbox",MODE_PRIVATE);
                    SharedPreferences.Editor editor=preferences.edit();
                    editor.putString("remember","false");
                    editor.apply();
                    Toast.makeText(home_screen.this,"Không nhớ mật khẩu..",Toast.LENGTH_SHORT).show();
                }
            }
        };
        remember.setOnCheckedChangeListener(rememberListener);

        // Lưu listener references
        signUpListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(home_screen.this,Registration.class);
                startActivity(intent);
            }
        };
        mSignUpHere.setOnClickListener(signUpListener);

        forgetPasswordListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(home_screen.this,resetpassword.class);
                startActivity(intent);
            }
        };
        mforget_password.setOnClickListener(forgetPasswordListener);

        loginListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();
                String pass = mPass.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    mEmail.setError("Chưa nhập email..",null);
                    return;
                }
                if (TextUtils.isEmpty(pass)) {
                    mPass.setError("Chưa nhập mật khẩu..",null);
                    return;
                }
                mDialog.setMessage("Đang đăng nhập..");
                mDialog.show();


                mAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (mDialog != null && mDialog.isShowing()) {
                            mDialog.dismiss();
                        }
                        if(task.isSuccessful()){
                            checkEmailVerification();
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(),"Đăng nhập thất bại..",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };
        btnLogin.setOnClickListener(loginListener);

    }
    private void checkEmailVerification()
    {
        FirebaseUser firebaseUser = mAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        
        Boolean emailflag = firebaseUser.isEmailVerified();
        if(emailflag)
        {
            Toast.makeText(getApplicationContext(),"Login Successful..",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(home_screen.this,first_home_page.class));
            finish();
        }
        else
        {
            Toast.makeText(this,"Please verify your email..",Toast.LENGTH_LONG).show();
            mAuth.signOut();
        }
    }
    // Trong home_screen.java

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove CheckBox listener
        if (remember != null && rememberListener != null) {
            remember.setOnCheckedChangeListener(null);
        }

        // Remove button listeners
        if (mSignUpHere != null && signUpListener != null) {
            mSignUpHere.setOnClickListener(null);
        }
        if (mforget_password != null && forgetPasswordListener != null) {
            mforget_password.setOnClickListener(null);
        }
        if (btnLogin != null && loginListener != null) {
            btnLogin.setOnClickListener(null);
        }

        // Giải phóng Dialog
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = null;

        // Cắt tham chiếu View
        mEmail = null;
        mPass = null;
        remember = null;
        btnLogin = null;
        mforget_password = null;
        mSignUpHere = null;
        
        // Cắt tham chiếu listeners
        rememberListener = null;
        signUpListener = null;
        forgetPasswordListener = null;
        loginListener = null;
    }
}