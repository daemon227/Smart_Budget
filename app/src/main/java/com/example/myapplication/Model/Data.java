package com.example.myapplication.Model;

//import androidx.recyclerview.widget.RecyclerView;

public class Data {
    //extends RecyclerView.ViewHolder
    private float amount;
    private Category type;
    private String note;
    private String id;

    public Data(float amount, Category type, String note, String id, String date)
    {

        this.amount = amount;
        this.type = type;
        this.note = note;
        this.id = id;
        this.date = date;
    }



    private String date;
    public Data(){}


    public float getAmount(){return amount;}

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Category getType() {
        return type;
    }

    public void setType(Category type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
