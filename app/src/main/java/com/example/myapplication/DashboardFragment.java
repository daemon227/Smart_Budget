package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Model.Category;
import com.example.myapplication.Model.Data;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.DateFormat;
import java.util.*;

public class DashboardFragment extends Fragment {

    private TextView statisticsText;

    private FloatingActionButton fabMain, fabIncomeBtn, fabExpenseBtn;
    private TextView fabIncomeTxt, fabExpenseTxt;
    private boolean isOpen = false;

    private TextView totalIncomeResult, totalExpenseResult, totalBalanceResult;
    private double totalSumIncome = 0;
    private double totalSumExpense = 0;

    private FirebaseAuth mAuth;
    private DatabaseReference mIncomeDatabase, mExpenseDatabase;

    private RecyclerView mRecyclerIncome, mRecyclerExpense;
    private Spinner spinnerCategory;
    private Category selectedCategory;
    private List<Category> listCategory = new ArrayList<>();

    private ValueEventListener totalsListener, statisticsListener;

    private static final int MAX_RECYCLER_ITEMS = 30;  // Giảm từ 50 xuống 30 để tránh OOM

    private boolean isShowingWarning = false;

    @Override
    public void onStop() {
        super.onStop();
        if (mIncomeDatabase != null && totalsListener != null)
            mIncomeDatabase.removeEventListener(totalsListener);

        if (mExpenseDatabase != null && totalsListener != null)
            mExpenseDatabase.removeEventListener(totalsListener);

        if (mExpenseDatabase != null && statisticsListener != null)
            mExpenseDatabase.removeEventListener(statisticsListener);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng View để không giữ tham chiếu Activity context
        if (mRecyclerIncome != null) mRecyclerIncome.setAdapter(null);
        // ... giải phóng các View khác
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();

        if (mUser == null) {
            if (isAdded()) {  // check Fragment đang gắn với Activity
                startActivity(new Intent(getActivity(), home_screen.class));
                requireActivity().finish();
            }
            return view; // không show Toast hay dialog
        }

        String uid = mUser.getUid();

        mIncomeDatabase = FirebaseDatabase.getInstance().getReference("Income").child(uid);
        mExpenseDatabase = FirebaseDatabase.getInstance().getReference("Expense").child(uid);

        initViews(view);
        setupRecyclerViews();
        loadTotals();
        loadStatisticsText();

        return view;
    }

    private void initViews(View view) {
        fabMain = view.findViewById(R.id.fb_main_lus_btn);
        fabIncomeBtn = view.findViewById(R.id.income_ft_btn);
        fabExpenseBtn = view.findViewById(R.id.expense_ft_btn);
        fabIncomeTxt = view.findViewById(R.id.income_ft_text);
        fabExpenseTxt = view.findViewById(R.id.expense_ft_text);

        totalIncomeResult = view.findViewById(R.id.income_set_result);
        totalExpenseResult = view.findViewById(R.id.expense_set_result);
        totalBalanceResult = view.findViewById(R.id.balance_set_result);

        statisticsText = view.findViewById(R.id.statistics_text);

        mRecyclerIncome = view.findViewById(R.id.recycler_income);
        mRecyclerExpense = view.findViewById(R.id.recycler_expense);
    }

    // ───── TOTAL CALCULATION ───────────────────────────────────
    private void loadTotals() {

        totalsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                totalSumIncome = 0;
                totalSumExpense = 0;

                // Income
                mIncomeDatabase.limitToLast(MAX_RECYCLER_ITEMS)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snap) {
                                for (DataSnapshot ds : snap.getChildren()) {
                                    Data d = ds.getValue(Data.class);
                                    if (d != null) totalSumIncome += d.getAmount();
                                }
                                totalIncomeResult.setText(String.valueOf(totalSumIncome));
                                updateBalance();
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });

                // Expense
                mExpenseDatabase.limitToLast(MAX_RECYCLER_ITEMS)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snap) {
                                for (DataSnapshot ds : snap.getChildren()) {
                                    Data d = ds.getValue(Data.class);
                                    if (d != null) totalSumExpense += d.getAmount();
                                }
                                totalExpenseResult.setText(String.valueOf(totalSumExpense));
                                updateBalance();
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        mIncomeDatabase.limitToLast(MAX_RECYCLER_ITEMS)
                .addListenerForSingleValueEvent(totalsListener);

        mExpenseDatabase.limitToLast(MAX_RECYCLER_ITEMS)
                .addListenerForSingleValueEvent(totalsListener);

    }

    private void updateBalance() {
        double balance = totalSumIncome - totalSumExpense;
        totalBalanceResult.setText(String.valueOf(balance));

        if (totalSumIncome == 0) return;

        if (balance <= 0.1 * totalSumIncome && balance > 0.05 * totalSumIncome)
            showBalanceWarning("Cảnh báo: Số dư chỉ còn dưới 10%!");

        else if (balance <= 0.05 * totalSumIncome && balance > 0)
            showBalanceWarning("Cảnh báo: Số dư chỉ còn dưới 5%!");

        else if (balance <= 0)
            showBalanceWarning("Cảnh báo: Số dư đã = 0 hoặc âm!");
    }

    private void showBalanceWarning(String message) {
        if (isShowingWarning) return;
        isShowingWarning = true;

        new AlertDialog.Builder(getActivity())
                .setTitle("Cảnh báo")
                .setMessage(message)
                .setPositiveButton("OK", (d, w) -> {
                    d.dismiss();
                    isShowingWarning = false;
                }).show();
    }

    // ───── STATISTICS TEXT ─────────────────────────────────────────
    private void loadStatisticsText() {
        if (statisticsText == null || mExpenseDatabase == null) return;

        statisticsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (statisticsText == null || !isAdded()) return;

                int transactionCount = 0;
                double totalExpense = 0;
                double maxExpense = 0;
                double minExpense = Double.MAX_VALUE;
                String maxDate = "";
                String minDate = "";

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Data data = ds.getValue(Data.class);
                    if (data != null) {
                        transactionCount++;
                        double amount = data.getAmount();
                        totalExpense += amount;

                        if (amount > maxExpense) {
                            maxExpense = amount;
                            maxDate = data.getDate();
                        }
                        if (amount < minExpense) {
                            minExpense = amount;
                            minDate = data.getDate();
                        }
                    }
                }

                // Format thống kê dạng text
                StringBuilder statsText = new StringBuilder();
                statsText.append("THỐNG KÊ CHI TIÊU\n\n");
                
                if (transactionCount > 0) {
                    double avgExpense = totalExpense / transactionCount;
                    statsText.append("• Tổng giao dịch: ").append(transactionCount).append("\n");
                    statsText.append("• Tổng chi tiêu: ").append(String.format("%.0f", totalExpense)).append(" đ\n");
                    statsText.append("• Chi tiêu trung bình: ").append(String.format("%.0f", avgExpense)).append(" đ\n");
                    
                    if (maxExpense > 0) {
                        statsText.append("• Chi tiêu cao nhất: ").append(String.format("%.0f", maxExpense)).append(" đ\n");
                        if (maxDate != null && !maxDate.isEmpty()) {
                            statsText.append("  Ngày: ").append(maxDate).append("\n");
                        }
                    }
                    
                    if (minExpense < Double.MAX_VALUE) {
                        statsText.append("• Chi tiêu thấp nhất: ").append(String.format("%.0f", minExpense)).append(" đ\n");
                        if (minDate != null && !minDate.isEmpty()) {
                            statsText.append("  Ngày: ").append(minDate);
                        }
                    }
                } else {
                    statsText.append("Chưa có dữ liệu chi tiêu");
                }

                statisticsText.setText(statsText.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (statisticsText != null && isAdded()) {
                    statisticsText.setText("Lỗi khi tải thống kê");
                }
            }
        };

        mExpenseDatabase.limitToLast(MAX_RECYCLER_ITEMS)
                .addValueEventListener(statisticsListener);
    }

    // ───── RECYCLER VIEW ─────────────────────────────────────────
    private void setupRecyclerViews() {
        mRecyclerIncome.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false)
        );
        mRecyclerExpense.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false)
        );
    }

    // ───── INSERT DATA ────────────────────────────────────────────
    private void incomeDataInsert() { initCategories(); showInsertDialog(true); }
    private void expenseDataInsert() { initCategories(); showInsertDialog(false); }

    private void initCategories() {
        if (!listCategory.isEmpty()) return;
        listCategory.add(new Category("001", "Lương"));
        listCategory.add(new Category("002", "Tiền thưởng"));
        listCategory.add(new Category("003", "Bán hàng"));
        listCategory.add(new Category("004", "Khác"));
    }

    private void showInsertDialog(boolean isIncome) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.custom_layout_for_insertdata, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        EditText edtAmount = view.findViewById(R.id.amount_edt);
        EditText edtNote = view.findViewById(R.id.note_edt);
        spinnerCategory = view.findViewById(R.id.spinner_category);

        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item, listCategory
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                selectedCategory = listCategory.get(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnSave.setOnClickListener(v -> {

            String strAmount = edtAmount.getText().toString().trim();
            String note = edtNote.getText().toString().trim();

            if (TextUtils.isEmpty(strAmount)) { edtAmount.setError("Required"); return; }
            if (TextUtils.isEmpty(note)) { edtNote.setError("Required"); return; }

            float amount;
            try { amount = Float.parseFloat(strAmount); }
            catch (Exception e) { edtAmount.setError("Sai định dạng số!"); return; }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {

                DatabaseReference db = isIncome ? mIncomeDatabase : mExpenseDatabase;
                String id = db.push().getKey();
                String dateStr = DateFormat.getDateInstance().format(new Date());

                Data data = new Data(amount, selectedCategory, note, id, dateStr);
                db.child(id).setValue(data);

                Toast.makeText(getActivity(), "Đã thêm!", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ───── FIREBASE ADAPTER ──────────────────────────────────────
    @Override
    public void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), home_screen.class));
            requireActivity().finish();
            return;
        }

        FirebaseRecyclerOptions<Data> incomeOpt =
                new FirebaseRecyclerOptions.Builder<Data>()
                        .setQuery(mIncomeDatabase.limitToLast(MAX_RECYCLER_ITEMS), Data.class)
                        .build();

        FirebaseRecyclerAdapter<Data, IncomeViewHolder> incomeAdapter =
                new FirebaseRecyclerAdapter<Data, IncomeViewHolder>(incomeOpt) {
                    @Override
                    protected void onBindViewHolder(@NonNull IncomeViewHolder holder,
                                                    int pos, @NonNull Data model) {
                        holder.setIncomeType(model.getType().getName());
                        holder.setIncomeAmount(model.getAmount());
                        holder.setIncomeDate(model.getDate());
                    }

                    @NonNull
                    @Override
                    public IncomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        return new IncomeViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.dashboard_income, parent, false)
                        );
                    }
                };

        mRecyclerIncome.setAdapter(incomeAdapter);
        incomeAdapter.startListening();

        FirebaseRecyclerOptions<Data> expenseOpt =
                new FirebaseRecyclerOptions.Builder<Data>()
                        .setQuery(mExpenseDatabase.limitToLast(MAX_RECYCLER_ITEMS), Data.class)
                        .build();

        FirebaseRecyclerAdapter<Data, ExpenseViewHolder> expenseAdapter =
                new FirebaseRecyclerAdapter<Data, ExpenseViewHolder>(expenseOpt) {
                    @Override
                    protected void onBindViewHolder(@NonNull ExpenseViewHolder holder,
                                                    int pos, @NonNull Data model) {
                        holder.setExpenseType(model.getType().getName());
                        holder.setExpenseAmount(model.getAmount());
                        holder.setExpenseDate(model.getDate());
                    }

                    @NonNull
                    @Override
                    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        return new ExpenseViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.dashboard_expense, parent, false)
                        );
                    }
                };

        mRecyclerExpense.setAdapter(expenseAdapter);
        expenseAdapter.startListening();
    }

    // ───── VIEW HOLDERS ──────────────────────────────────────────
    public static class IncomeViewHolder extends RecyclerView.ViewHolder {
        View v;
        public IncomeViewHolder(View itemView) { super(itemView); v = itemView; }
        public void setIncomeType(String type) { ((TextView)v.findViewById(R.id.type_Income_ds)).setText(type); }
        public void setIncomeAmount(double amount) { ((TextView)v.findViewById(R.id.amount_Income_ds)).setText(String.valueOf(amount)); }
        public void setIncomeDate(String date) { ((TextView)v.findViewById(R.id.date_Income_ds)).setText(date); }
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        View v;
        public ExpenseViewHolder(View itemView) { super(itemView); v = itemView; }
        public void setExpenseType(String type) { ((TextView)v.findViewById(R.id.type_Expense_ds)).setText(type); }
        public void setExpenseAmount(double amount) { ((TextView)v.findViewById(R.id.amount_Expense_ds)).setText(String.valueOf(amount)); }
        public void setExpenseDate(String date) { ((TextView)v.findViewById(R.id.date_Expense_ds)).setText(date); }
    }
}
