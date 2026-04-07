package com.qarz.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qarz.app.R;
import com.qarz.app.models.DisplayLoan;

import java.util.List;

public class TabLoanAdapter extends RecyclerView.Adapter<TabLoanAdapter.ViewHolder> {

    private List<DisplayLoan> loansList;

    public TabLoanAdapter(List<DisplayLoan> loansList) {
        this.loansList = loansList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tab_loan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DisplayLoan loan = loansList.get(position);
        holder.tvName.setText(loan.getDisplayTitle());
        holder.tvDesc.setText(loan.getDescription() != null && !loan.getDescription().trim().isEmpty() ? loan.getDescription() : "No description");
        holder.tvAmount.setText(String.format("Rs. %.2f", loan.getAmount()));
        
        holder.tvUid.setText("UID: " + loan.getLoanId());
        holder.ivCopy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) v.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Loan UID", loan.getLoanId());
            clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(v.getContext(), "UID Copied to Clipboard", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return loansList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvAmount, tvUid;
        android.widget.ImageView ivCopy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTabLoanName);
            tvDesc = itemView.findViewById(R.id.tvTabLoanDesc);
            tvAmount = itemView.findViewById(R.id.tvTabLoanAmount);
            tvUid = itemView.findViewById(R.id.tvTabLoanUid);
            ivCopy = itemView.findViewById(R.id.ivTabCopyUid);
        }
    }
}
