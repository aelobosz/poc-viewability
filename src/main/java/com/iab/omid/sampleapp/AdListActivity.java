package com.iab.omid.sampleapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import com.iab.omid.sampleapp.content.TestPages;
import java.util.List;

/**
 * An activity representing a list of ads. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link AdDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class AdListActivity extends AppCompatActivity {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ad_list);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setTitle(getTitle());

		if (findViewById(R.id.ad_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-w900dp).
			// If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;
		}

		View recyclerView = findViewById(R.id.ad_list);
		assert recyclerView != null;
		setupRecyclerView((RecyclerView)recyclerView);
	}

	private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
		recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this, TestPages.ITEMS, mTwoPane));
	}

	public static class SimpleItemRecyclerViewAdapter
			extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

		private final AdListActivity mParentActivity;
		private final List<TestPages.TestPage> mValues;
		private final boolean mTwoPane;
		private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				TestPages.TestPage item = (TestPages.TestPage)view.getTag();
				if (mTwoPane) {
					AdDetailActivity.addTestAdFragment(item, mParentActivity.getSupportFragmentManager(), item.id);
				} else {
					Context context = view.getContext();
					Intent intent = new Intent(context, AdDetailActivity.class);
					intent.putExtra(AdDetailActivity.ARG_ITEM_ID, item.id);

					context.startActivity(intent);
				}
			}
		};

		SimpleItemRecyclerViewAdapter(AdListActivity parent,
									  List<TestPages.TestPage> items,
									  boolean twoPane) {
			mValues = items;
			mParentActivity = parent;
			mTwoPane = twoPane;
		}

		@NonNull
        @Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.ad_list_content, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
			holder.mContentView.setText(mValues.get(position).getTitle());

			holder.itemView.setTag(mValues.get(position));
			holder.itemView.setOnClickListener(mOnClickListener);
		}

		@Override
		public int getItemCount() {
			return mValues.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			final TextView mContentView;

			ViewHolder(View view) {
				super(view);
				mContentView = view.findViewById(R.id.content);
			}
		}
	}
}
