package com.example.phonebook_esc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Adapter.Holder>  {

    private Context context;
    private ArrayList<Contact> datalist; //datalist 연결하기 위해서

    public Adapter(Context context, ArrayList<Contact> datalist) {
        this.context = context;
        this.datalist = datalist;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recy_item, parent, false);
        Holder holder = new Holder(view);
        return holder;
    }
    //생성된 viewHolder를 bind 해주는! data를 새롭게 불러온다.
    @Override
    public void onBindViewHolder(@NonNull Holder holder, @SuppressLint("RecyclerView") int position) {
        Contact contact = datalist.get(position);
        holder.name.setText(contact.getName());
        holder.phNum.setText(contact.getPhNum());

        //클릭이 되었을 때, 아래 내용을 실행
        holder.call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("전화를 걸지, 문자를 보낼지 선택해 주십시오.");
                builder.setMessage("골라줘!");
                builder.setNeutralButton("전화걸기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        String number = datalist.get(position).getPhNum();
                        Uri numberU = Uri.parse("tel:"+number);
                        Intent call = new Intent(Intent.ACTION_CALL, numberU);
                        context.startActivity(call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
                builder.setPositiveButton("문자 보내기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri smsUri = Uri.parse("tel:"+contact.getPhNum());
                        Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
                        intent.putExtra("address", contact.getPhNum());
                        intent.putExtra("sms_body", "");
                        intent.setType("vnd.android-dir/mms-sms");
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
                builder.create().show();
            }
        });
        holder.item_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("삭제");
                builder.setMessage("정말 삭제하시겠습니까?");
                builder.setNegativeButton("아니여", null);
                builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteContactFroNumber(context.getContentResolver(), datalist.get(position).getPhNum());
                        datalist.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, datalist.size());

                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return datalist.size();
    }

    public class Holder extends RecyclerView.ViewHolder {

        protected ConstraintLayout item_container;
        protected TextView name;
        protected TextView phNum;
        protected Button call;

        public Holder(@NonNull View itemView) {
            super(itemView);
            //연결하기
            item_container = itemView.findViewById(R.id.item_container);
            name = itemView.findViewById(R.id.name);
            phNum = itemView.findViewById(R.id.PhoneNum);
            call = itemView.findViewById(R.id.call);
        }
    }

    public void filterList(ArrayList<Contact> filiteredList) {
        datalist = filiteredList;
        notifyDataSetChanged();
    }

    @SuppressLint("Range")
    private static long getContactIdFromNumber(ContentResolver contactHelper, String number) {
        Uri contactUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));

        String[] projection = {ContactsContract.PhoneLookup._ID};

        Cursor cursor = contactHelper.query(contactUri, projection, null, null, null);

        if(cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
        } else if(cursor != null) {
            cursor.close();
        }
        return -1;
    }

    public static void deleteContactFroNumber(ContentResolver contactHelper, String number) {
        ArrayList <ContentProviderOperation> ops = new ArrayList<>();
        String[] whereArgs = new String[] {String.valueOf(getContactIdFromNumber(contactHelper, number))};

        ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                .withSelection(ContactsContract.RawContacts.CONTACT_ID + "=?", whereArgs).build());
        try {
            contactHelper.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}
