package com.example.myapplication;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.myapplication.Model.Data;

import java.util.ArrayList;

public class SearchResultAdapter extends BaseAdapter {

    Context context;
    ArrayList<Data> list;
    LayoutInflater inflater;

    public SearchResultAdapter(Context context, ArrayList<Data> list) {
        this.context = context;
        this.list = list;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() { return list.size(); }

    @Override
    public Object getItem(int position) { return list.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_search_result, null);

        TextView txtAmount = view.findViewById(R.id.txtAmount);
        TextView txtType = view.findViewById(R.id.txtType);
        TextView txtNote = view.findViewById(R.id.txtNote);
        TextView txtDate = view.findViewById(R.id.txtDate);

        Data d = list.get(position);

        txtAmount.setText(d.getAmount() + " đ");
        txtType.setText("Loại: " + d.getType());
        txtNote.setText("Ghi chú: " + d.getNote());
        txtDate.setText("Ngày: " + d.getDate());

        return view;
    }
}
