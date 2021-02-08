package com.alphawallet.app.ui.widget.holder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.TokenManagementActivity;
import com.alphawallet.app.ui.widget.entity.GroupTokenData;
import com.alphawallet.app.util.VelasUtils;
import com.alphawallet.app.widget.CopyTextView;

import static com.alphawallet.app.C.EXTRA_ADDRESS;
import static com.alphawallet.app.C.EXTRA_CHAIN_ID;

public class GroupTokensHolder extends BinderViewHolder<GroupTokenData> {

    private ViewGroup sectionLayout;
    private TextView sectionName;
    private TextView address;
    private ImageView ivAdd;
    private ImageView ivCopy;

    @Override
    public void bind(@Nullable GroupTokenData data, @NonNull Bundle addition) {
        sectionName.setText(data.groupName);
        address.setText(VelasUtils.isVelasNetwork(data.getChainId()) ? VelasUtils.ethToVlx(data.walletAddress) : data.walletAddress);
        ivAdd.setOnClickListener(v -> {
            if (data.walletAddress != null) {
                Intent intent = new Intent(getContext(), TokenManagementActivity.class);
                intent.putExtra(EXTRA_ADDRESS, data.walletAddress);
                intent.putExtra(EXTRA_CHAIN_ID, data.getChainId());
                getContext().startActivity(intent);
            }
        });
        if (data.getSectionColor() == 0) {
            sectionLayout.setBackgroundColor(getContext().getResources().getColor(R.color.light_gray));
        } else {
            sectionLayout.setBackgroundColor(data.getSectionColor());
        }

        ivCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(CopyTextView.KEY_ADDRESS, address.getText());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        });
    }

    public GroupTokensHolder(int res_id, ViewGroup parent) {
        super(res_id, parent);
        sectionLayout = findViewById(R.id.llSection);
        sectionName = findViewById(R.id.tvSectionName);
        address = findViewById(R.id.tvAddress);
        ivAdd = findViewById(R.id.ivPlus);
        ivCopy = findViewById(R.id.img_copy);
    }
}
