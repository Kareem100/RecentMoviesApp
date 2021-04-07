package com.example.recentmovies;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import Loaders.MovieLoader;
import MoviesData.Movie;
import MoviesData.MoviesAdapter;
import Receivers.NetworkStateReceiver;

public class MainActivity extends AppCompatActivity implements MoviesAdapter.MovieClickListener,
        LoaderManager.LoaderCallbacks<ArrayList<Movie>>, NetworkStateReceiver.NetworkStateReceiverListener {

    private static final String JSON_QUERY_LINK = "https://content.guardianapis.com/search";

    private final int MOVIES_LOADER_ID = 0;
    private NetworkStateReceiver networkStateReceiver;
    private RecyclerView moviesRecyclerView;
    private MoviesAdapter moviesAdapter;
    private TextView tipTextView;
    private TextView extraTipTextView;
    private ImageView tipImageView;
    private LinearLayout tipContainerLinearLayout;
    private ProgressBar circularProgress;
    private boolean savedData;
    private boolean savedDataOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        centerTitle();
        setHooks();
        savedData = savedDataOnce = false;

        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void setHooks() {
        circularProgress = findViewById(R.id.progress_circular);
        tipContainerLinearLayout = findViewById(R.id.tip_container_linear_layout);
        tipTextView = findViewById(R.id.tip_text_view);
        extraTipTextView = findViewById(R.id.extra_tip_text_view);
        tipImageView = findViewById(R.id.tip_image_view);
        moviesRecyclerView = findViewById(R.id.movies_recycler_view);
        moviesAdapter = new MoviesAdapter(this, new ArrayList<>(), this);
        moviesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        moviesRecyclerView.setAdapter(moviesAdapter);
    }

    private void centerTitle() {
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView titleTextView = (TextView)
                TextView.inflate(this, R.layout.center_title_text_view, null);
        titleTextView.setLayoutParams(new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(titleTextView);
    }

    private String buildUri() {
        Uri baseUri = Uri.parse(JSON_QUERY_LINK);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        String movieTag = getString(R.string.filter_movies_tag);
        String fromDate = getString(R.string.filter_from_date);
        String showedTags = getString(R.string.filter_show_tags);
        String showedFields = getString(R.string.filter_show_fields);
        String orderBy = getString(R.string.filter_order_by);

        uriBuilder.appendQueryParameter("formant", "json");
        uriBuilder.appendQueryParameter("tag", movieTag);
        uriBuilder.appendQueryParameter("from-date", fromDate);
        uriBuilder.appendQueryParameter("show-tags", showedTags);
        uriBuilder.appendQueryParameter("show-fields", showedFields);
        uriBuilder.appendQueryParameter("order-by", orderBy);
        uriBuilder.appendQueryParameter("api-key", BuildConfig.API_KEY);

        return uriBuilder.toString();
    }

    @Override
    public void onMovieClick(int position) {
        // open the review made by the author
        try {
            String reviewLink = moviesAdapter.getReviewLink(position);
            Uri reviewUri = Uri.parse(reviewLink);
            Intent reviewWebsiteIntent = new Intent(Intent.ACTION_VIEW, reviewUri);
            startActivity(reviewWebsiteIntent);
        } catch (Exception e) {
            Log.e(MainActivity.class.getName(), "MainActivity.onMovieClick: " +
                    "Problem Opening The Website.", e);
        }
    }

    @NonNull
    @Override
    public Loader<ArrayList<Movie>> onCreateLoader(int id, @Nullable Bundle args) {
        String jsonQueryLink = buildUri();
        return new MovieLoader(this, jsonQueryLink);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<Movie>> loader, ArrayList<Movie> data) {
        // clear the adapter of previous earthquake data
        moviesAdapter.clearData();

        if (data != null && !data.isEmpty()) {
            moviesAdapter.setData(data);
            moviesAdapter.notifyDataSetChanged();
            circularProgress.setVisibility(View.GONE);
            tipContainerLinearLayout.setVisibility(View.GONE);
            moviesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            tipTextView.setText(R.string.empty_data_state_text);
            extraTipTextView.setText(R.string.extra_empty_data_state_text);
            tipImageView.setImageResource(R.drawable.ic_try_later);
            circularProgress.setVisibility(View.GONE);
            tipContainerLinearLayout.setVisibility(View.VISIBLE);
            moviesRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<Movie>> loader) {
        moviesAdapter.clearData();
    }

    @Override
    public void onAvailableNetwork() {
        if (!savedDataOnce) {
            tipContainerLinearLayout.setVisibility(View.GONE);
            circularProgress.setVisibility(View.VISIBLE);
            savedData = true;
            savedDataOnce = true;
            getSupportLoaderManager().initLoader(MOVIES_LOADER_ID, null, this);
        }
    }

    @Override
    public void onUnavailableNetwork() {
        if (!savedData) {
            tipTextView.setText(R.string.no_network_state_text);
            extraTipTextView.setText(R.string.extra_no_network_state_text);
            tipImageView.setImageResource(R.drawable.ic_wifi_off);
            circularProgress.setVisibility(View.GONE);
            moviesRecyclerView.setVisibility(View.GONE);
            tipContainerLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
    }
}