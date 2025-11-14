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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class IncomeFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mIncomeDatabase;
    private RecyclerView recyclerView;

    private TextView incomeTotalSum;

    ///Update edit text.

    private EditText edtAmount;
    private EditText edtType;
    private EditText edtNote;

    //button for update and delete

    private Button btnUpdate;
    private Button btnDelete;

    //Data item value
    private String type;
    private String note;
    private float amount;

    private  String post_key;
    Spinner spinnerCategory;
    Category selectedCategory;
    List<Category> listCategory = new ArrayList<>();
    private ValueEventListener incomeListener;

    private static final int MAX_RECYCLER_ITEMS = 50;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View myview=inflater.inflate(R.layout.fragment_income, container, false);
        mAuth=FirebaseAuth.getInstance();
        FirebaseUser mUser=mAuth.getCurrentUser();
        
        if (mUser == null) {
            return myview;
        }

        String uid = mUser.getUid();
        mIncomeDatabase = FirebaseDatabase.getInstance().getReference().child("Income").child(uid);

        incomeTotalSum=myview.findViewById(R.id.income_txt_result);
        recyclerView=myview.findViewById(R.id.recycler_id_income);
        LinearLayoutManager layoutManager=new LinearLayoutManager(getActivity());

        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        // Khởi tạo listener TRƯỚC khi sử dụng
        incomeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalvalue = 0;
                for(DataSnapshot mysnapshot : dataSnapshot.getChildren()){
                    Data data = mysnapshot.getValue(Data.class);
                    if (data != null)
                        totalvalue += data.getAmount();
                }
                if (incomeTotalSum != null) {
                    incomeTotalSum.setText(String.valueOf(totalvalue));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        
        // Thêm listener với giới hạn dữ liệu
        mIncomeDatabase.limitToLast(MAX_RECYCLER_ITEMS).addValueEventListener(incomeListener);


        return  myview;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mIncomeDatabase == null) {
            return;
        }

        FirebaseRecyclerOptions<Data> options =
                new FirebaseRecyclerOptions.Builder<Data>()
                        .setQuery(mIncomeDatabase.limitToLast(MAX_RECYCLER_ITEMS), Data.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Data, MyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull Data model) {

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
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.income_recycler_data, parent, false);
                return new MyViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private FirebaseRecyclerAdapter<Data, MyViewHolder> adapter;

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
        if (mIncomeDatabase != null && incomeListener != null) {
            mIncomeDatabase.removeEventListener(incomeListener);
        }
    }
    // Trong mỗi Fragment (ví dụ: ExpenseFragment.java)

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Giải phóng RecyclerView và Adapter
        // Adapter thường giữ tham chiếu đến Context.
        if (recyclerView != null) {
            recyclerView.setAdapter(null); // Rất quan trọng!
            recyclerView = null;
        }

        // Giải phóng các View lớn khác (ví dụ: Chart, Button, TextView...)
        incomeTotalSum = null;
        edtAmount = null;
        edtNote = null;
        spinnerCategory = null;

        // 💡 Hành động mới: Nếu Fragment của bạn có tham chiếu đến các Bitmap/Drawable lớn,
        // hãy đặt chúng thành null ở đây.
    }



    public static class MyViewHolder extends RecyclerView.ViewHolder{

        View mView;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
        }

        private void setType(String type){
            TextView mType=mView.findViewById(R.id.type_txt_income);
            mType.setText(type);
        }
        private void setNote(String note){
            TextView mNote=mView.findViewById(R.id.note_txt_income);
            mNote.setText(note);
        }
        private void setDate(String date){
            TextView mDate=mView.findViewById(R.id.date_txt_income);
            mDate.setText(date);
        }
        private void setAmount(int amount){
            TextView mAmount=mView.findViewById(R.id.amount_txt_income);
            String stamount=String.valueOf(amount);
            mAmount.setText(stamount);
        }
    }

    private void updateDataItem()
    {
        listCategory.clear();
        listCategory.add(new Category("001", "Lương"));
        listCategory.add(new Category("002", "Tiền thưởng"));
        listCategory.add(new Category("003", "Bán hàng"));
        listCategory.add(new Category("004", "Khác"));

        if (getActivity() == null || !isAdded()) return;
        AlertDialog.Builder mydialog=new AlertDialog.Builder(getActivity());
        LayoutInflater inflater=LayoutInflater.from(getActivity());
        View myview=inflater.inflate(R.layout.update_data_item,null);
        mydialog.setView(myview);
        spinnerCategory = myview.findViewById(R.id.spinner_category);

        edtAmount=myview.findViewById(R.id.amount_edt);
//        edtType=myview.findViewById(R.id.type_edt);
        edtNote=myview.findViewById(R.id.note_edt);

        //Set data to edit text..
        //edtType.setText(type);
        //edtType.setSelection(type.length());

        edtNote.setText(note);
        edtNote.setSelection(note.length());

        edtAmount.setText(String.valueOf(amount));
        edtAmount.setSelection(String.valueOf(amount).length());

        btnUpdate=myview.findViewById(R.id.btn_upd_Update);
        btnDelete=myview.findViewById(R.id.btnuPD_Delete);

        AlertDialog dialog=mydialog.create();
        spinnerCategory = myview.findViewById(R.id.spinner_category);

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
                //type=edtType.getText().toString().trim();
                note=edtNote.getText().toString().trim();

                String mdAmount=String.valueOf(amount);
                mdAmount=edtAmount.getText().toString().trim();

                int myAmount=Integer.parseInt(mdAmount);

                String mDate= DateFormat.getDateInstance().format(new Date());

                Data data=new Data(myAmount,selectedCategory,note,post_key,mDate);

                mIncomeDatabase.child(post_key).setValue(data);

                dialog.dismiss();
            }
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa mục thu nhập này không?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        mIncomeDatabase.child(post_key).removeValue();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        dialog.show();

    }
}