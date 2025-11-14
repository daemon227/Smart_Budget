package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Model.Category;
import com.example.myapplication.Model.Data;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ExpenseFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mExpenseDatabase;
    private RecyclerView recyclerView;

    private TextView expenseTotalSum;

    private EditText edtAmount;
    private Spinner edtType;
    private EditText edtNote;

    private Button btnUpdate;
    private Button btnDelete;

    private String type;
    private String note;
    private float amount;

    private  String post_key;
    Spinner spinnerCategory;
    Category selectedCategory;
    List<Category> listCategory = new ArrayList<>();
    private ValueEventListener expenseListener;
    private FirebaseRecyclerAdapter<Data, MyViewHolder> adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View myview=inflater.inflate(R.layout.fragment_expense, container, false);
        mAuth=FirebaseAuth.getInstance();
        FirebaseUser mUser=mAuth.getCurrentUser();
        if(mAuth!=null) {
            String uid = mUser.getUid();

            mExpenseDatabase = FirebaseDatabase.getInstance().getReference().child("ExpenseData").child(uid);
        }

        expenseTotalSum=myview.findViewById(R.id.expense_txt_result);
        recyclerView=myview.findViewById(R.id.recycler_id_expense);
        LinearLayoutManager layoutManager=new LinearLayoutManager(getActivity());

        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);


        expenseListener = new ValueEventListener() {

            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalvalue = 0;
                for (DataSnapshot mysnapshot : dataSnapshot.getChildren()) {
                    Data data = mysnapshot.getValue(Data.class);
                    if (data != null)
                        totalvalue += data.getAmount();
                }
                expenseTotalSum.setText(String.valueOf(totalvalue));
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        mExpenseDatabase.addValueEventListener(expenseListener);

        return  myview;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Data> options =
                new FirebaseRecyclerOptions.Builder<Data>()
                        .setQuery(mExpenseDatabase, Data.class)
                        .build();

        FirebaseRecyclerAdapter<Data, ExpenseFragment.MyViewHolder> adapter =
                new FirebaseRecyclerAdapter<Data, ExpenseFragment.MyViewHolder>(options) {

                    @Override
                    protected void onBindViewHolder(@NonNull ExpenseFragment.MyViewHolder holder, int position, @NonNull Data model) {

                        holder.setType(model.getType().getName());
                        holder.setNote(model.getNote());
                        holder.setDate(model.getDate());
                        holder.setAmount((int) model.getAmount());

                        holder.mView.setOnClickListener(v -> {
                            post_key = getRef(position).getKey();

                            type   = model.getType().getName();
                            note   = model.getNote();
                            amount = model.getAmount();

                            updateDataItem();
                        });
                    }

                    @NonNull
                    @Override
                    public ExpenseFragment.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.expense_recycler_data, parent, false);
                        return new ExpenseFragment.MyViewHolder(view);
                    }
                };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mExpenseDatabase != null && expenseListener != null)
            mExpenseDatabase.removeEventListener(expenseListener);
        if (adapter != null) adapter.stopListening();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        View mView;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
        }

        private void setType(String type){
            TextView mType=mView.findViewById(R.id.type_txt_expense);
            mType.setText(type);
        }
        private void setNote(String note){
            TextView mNote=mView.findViewById(R.id.note_txt_expense);
            mNote.setText(note);
        }
        private void setDate(String date){
            TextView mDate=mView.findViewById(R.id.date_txt_expense);
            mDate.setText(date);
        }
        private void setAmount(int amount){
            TextView mAmount=mView.findViewById(R.id.amount_txt_expense);
            //String stamount=String.valueOf(amount);
            //mAmount.setText("-"+stamount);
            NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
            mAmount.setText("-" + nf.format(amount));
        }
    }

    private void updateDataItem() {
        listCategory.clear();
        listCategory.add(new Category("001", "Ăn uống"));
        listCategory.add(new Category("002", "Đi lại"));
        listCategory.add(new Category("003", "Mua sắm"));
        listCategory.add(new Category("004", "Khác"));

        AlertDialog.Builder mydialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View myview = inflater.inflate(R.layout.update_data_item, null);
        mydialog.setView(myview);

        edtAmount = myview.findViewById(R.id.amount_edt);
        //edtType = myview.findViewById(R.id.spinner_category);
        edtNote = myview.findViewById(R.id.note_edt);

        edtNote.setText(note);
        edtNote.setSelection(note.length());

        edtAmount.setText(String.valueOf(amount));
        edtAmount.setSelection(String.valueOf(amount).length());

        btnUpdate = myview.findViewById(R.id.btn_upd_Update);
        btnDelete = myview.findViewById(R.id.btnuPD_Delete);

        AlertDialog dialog = mydialog.create();

        spinnerCategory = myview.findViewById(R.id.spinner_category);
        if (getActivity() == null || mExpenseDatabase == null) return;


        ArrayAdapter<Category> adapter = new ArrayAdapter<>(

                getActivity(),
                android.R.layout.simple_spinner_item,
                listCategory
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Set giá trị hiện tại của spinner theo category của model
        int selectedIndex = 0;
        for (int i = 0; i < listCategory.size(); i++) {
            if (listCategory.get(i).getName().equals(type)) { // type là tên category cũ
                selectedIndex = i;
                break;
            }
        }
        spinnerCategory.setSelection(selectedIndex);
        selectedCategory = listCategory.get(selectedIndex);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = (Category) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //type = edtType.getText().toString().trim();
                note = edtNote.getText().toString().trim();
                String mdAmount = String.valueOf(amount);
                mdAmount = edtAmount.getText().toString().trim();
                if (mdAmount.isEmpty()) {
                    edtAmount.setError("Vui lòng nhập số tiền");
                    return;
                }
                int myAmount;
                try {
                    myAmount = Integer.parseInt(mdAmount);
                } catch (NumberFormatException e) {
                    edtAmount.setError("Số tiền không hợp lệ!");
                    return;
                }


                String mDate = DateFormat.getDateInstance().format(new Date());
                Data data = new Data(myAmount, selectedCategory, note, post_key, mDate);
                mExpenseDatabase.child(post_key).setValue(data);
                dialog.dismiss();
                mExpenseDatabase.addListenerForSingleValueEvent(expenseListener);
            }
        });


        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc muốn xóa mục này không?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        mExpenseDatabase.child(post_key).removeValue();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            mExpenseDatabase.addListenerForSingleValueEvent(expenseListener);
        });
    }
}
