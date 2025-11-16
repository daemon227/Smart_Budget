package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.myapplication.Model.Data;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class searchdata2 extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference mref;

    AutoCompleteTextView txtSearch;
    ListView listdata;
    EditText edtSingleDate, edtStartDate, edtEndDate, edtMinAmount, edtMaxAmount;
    Button btnSearchDate, btnSearchBetween, btnSearchAmount;

    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchdata);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            mref = FirebaseDatabase.getInstance().getReference("ExpenseData").child(user.getUid());
        }

        txtSearch = findViewById(R.id.txtSearch);
        listdata = findViewById(R.id.listdata);

        edtSingleDate = findViewById(R.id.edt_single_date);
        edtStartDate = findViewById(R.id.edt_start_date);
        edtEndDate = findViewById(R.id.edt_end_date);
        edtMinAmount = findViewById(R.id.edt_min_amount);
        edtMaxAmount = findViewById(R.id.edt_max_amount);

        btnSearchDate = findViewById(R.id.btn_search_date);
        btnSearchBetween = findViewById(R.id.btn_search_between);
        btnSearchAmount = findViewById(R.id.btn_search_amount);

        setDatePicker(edtSingleDate);
        setDatePicker(edtStartDate);
        setDatePicker(edtEndDate);

        loadAutoCompleteDates();

        btnSearchDate.setOnClickListener(v -> searchSingleDate());
        btnSearchBetween.setOnClickListener(v -> searchBetweenDates());
        btnSearchAmount.setOnClickListener(v -> searchAmount());

        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());
    }

    private void setDatePicker(EditText edt) {
        edt.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(y, m, d);

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                edt.setText(sdf.format(cal.getTime()));

            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }


    private long toMillis(String date) {
        try {
            return sdf.parse(date).getTime();
        } catch (Exception e) {
            return -1;
        }
    }

    private void loadAutoCompleteDates() {
        mref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> names = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Data data = ds.getValue(Data.class);
                    names.add(data.getDate());
                }

                ArrayAdapter adapter = new ArrayAdapter(searchdata2.this,
                        android.R.layout.simple_list_item_1, names);

                txtSearch.setAdapter(adapter);

                txtSearch.setOnItemClickListener((parent, view, position, id) -> {
                    searchExactDate(txtSearch.getText().toString());
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void searchExactDate(String date) {
        searchByFilter(data -> data.getDate().equals(date));
    }

    private void searchSingleDate() {
        String date = edtSingleDate.getText().toString();
        searchByFilter(data -> data.getDate().equals(date));
    }

    private void searchBetweenDates() {
        long start = toMillis(edtStartDate.getText().toString());
        long end = toMillis(edtEndDate.getText().toString());

        searchByFilter(data -> {
            long d = toMillis(data.getDate());
            return d >= start && d <= end;
        });
    }

    private void searchAmount() {
        int min = Integer.parseInt(edtMinAmount.getText().toString());
        int max = Integer.parseInt(edtMaxAmount.getText().toString());

        searchByFilter(data -> data.getAmount() >= min && data.getAmount() <= max);
    }

    private interface Filter {
        boolean match(Data data);
    }

    private void searchByFilter(Filter filter) {
        mref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                ArrayList<Data> results = new ArrayList<>();

                // Lọc dữ liệu
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Data data = ds.getValue(Data.class);

                    if (data != null && filter.match(data)) {
                        results.add(data);
                    }
                }

                // Gắn adapter đẹp
                SearchResultAdapter adapter = new SearchResultAdapter(searchdata2.this, results);
                listdata.setAdapter(adapter);

                // Nếu rỗng → báo người dùng
                if (results.isEmpty()) {
                    Toast.makeText(searchdata2.this, "Không có dữ liệu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

}