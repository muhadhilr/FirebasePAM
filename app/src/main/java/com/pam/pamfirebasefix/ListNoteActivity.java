package com.pam.pamfirebasefix;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ListNoteActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private List<Note> noteList;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_note);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("notes").child(currentUser.getUid());

        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList);
        recyclerView.setAdapter(noteAdapter);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                noteList.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Note note = postSnapshot.getValue(Note.class);
                    if (note != null) {
                        note.setKey(postSnapshot.getKey());
                        noteList.add(note);
                    }
                }
                noteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ListNoteActivity.this, "Failed to load notes.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ViewHolder for RecyclerView
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewDescription;
        Button btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    // Adapter for RecyclerView
    public class NoteAdapter extends RecyclerView.Adapter<NoteViewHolder> {

        private List<Note> noteList;

        public NoteAdapter(List<Note> noteList) {
            this.noteList = noteList;
        }

        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_note, parent, false);
            return new NoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            Note note = noteList.get(position);
            holder.textViewTitle.setText(note.getTitle());
            holder.textViewDescription.setText(note.getDescription());

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteNote(holder.getAdapterPosition());
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditDialog(holder.getAdapterPosition());
                }
            });
        }


        private void showEditDialog(int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ListNoteActivity.this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_edit_note, null);
            builder.setView(dialogView);

            EditText etTitle = dialogView.findViewById(R.id.et_title);
            EditText etDesc = dialogView.findViewById(R.id.et_description);

            Note note = noteList.get(position);
            etTitle.setText(note.getTitle());
            etDesc.setText(note.getDescription());

            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newTitle = etTitle.getText().toString().trim();
                    String newDesc = etDesc.getText().toString().trim();

                    if (!TextUtils.isEmpty(newTitle) && !TextUtils.isEmpty(newDesc)) {
                        updateNote(position, newTitle, newDesc);
                    } else {
                        Toast.makeText(ListNoteActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        private void updateNote(int position, String newTitle, String newDesc) {
            Note note = noteList.get(position);
            DatabaseReference itemRef = databaseReference.child(note.getKey());
            note.setTitle(newTitle);
            note.setDescription(newDesc);
            itemRef.setValue(note).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        notifyDataSetChanged();
                        Toast.makeText(ListNoteActivity.this, "Note updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ListNoteActivity.this, "Failed to update note", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return noteList.size();
        }

        private void deleteNote(int position) {
            if (position != RecyclerView.NO_POSITION && !noteList.isEmpty()) {
                String key = noteList.get(position).getKey();
                if (key != null) {
                    DatabaseReference itemRef = databaseReference.child(key);
                    itemRef.removeValue(new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Toast.makeText(ListNoteActivity.this, "Failed to delete note.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                noteList.remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(ListNoteActivity.this, "Note deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            } else {
                Toast.makeText(ListNoteActivity.this, "No notes available to delete",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
