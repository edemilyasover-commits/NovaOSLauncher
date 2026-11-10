package com.novaos.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    Handler handler = new Handler();
    LinearLayout root, grid;
    TextView clock, date;
    EditText search;
    boolean drawer = false, dark;
    ArrayList<ResolveInfo> apps = new ArrayList<>();
    Runnable ticker;

    int ink(){ return dark ? Color.WHITE : 0xFF18334A; }
    int muted(){ return dark ? 0xFFB9CBD9 : 0xFF5D7890; }
    int panel(){ return dark ? 0x554E7694 : 0x99FFFFFF; }
    GradientDrawable bg(int color,int stroke,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{color,dark?0x333B627F:0x66FFFFFF});
        d.setCornerRadius(radius); d.setStroke(1,stroke); return d;
    }
    TextView label(String s,int size,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(ink());
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    LinearLayout vertical(){ LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    LinearLayout horizontal(){ LinearLayout l=new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    LinearLayout.LayoutParams lp(int w,int h){ return new LinearLayout.LayoutParams(w,h); }
    View glass(View v,int radius){
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3378D8FF),bg(panel(),dark?0x667DB4D2:0xCCFFFFFF,radius),null));
        v.setElevation(7); return v;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(dark?0xFF071321:0xFFD9ECF8);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildHome(); loadApps();
        ticker=new Runnable(){public void run(){
            if(clock!=null)clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));
            if(date!=null)date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()));
            handler.postDelayed(this,1000);
        }};
        handler.post(ticker);
    }
    void buildHome(){
        drawer=false; root=vertical(); root.setPadding(22,24,22,16);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,dark?new int[]{0xFF071321,0xFF1A3B55,0xFF0B1D30}:new int[]{0xFFEAF7FF,0xFFB9D9EF,0xFFF9FCFF}));
        setContentView(root);
        TextView brand=label("N O V A   O S",12,true); brand.setTextColor(muted()); brand.setGravity(Gravity.CENTER);
        root.addView(brand,lp(-1,30));
        LinearLayout time=vertical(); time.setGravity(Gravity.CENTER); time.setPadding(18,26,18,24);
        clock=label(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),66,false); clock.setGravity(Gravity.CENTER);
        date=label(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()),15,false); date.setTextColor(muted()); date.setGravity(Gravity.CENTER);
        time.addView(clock); time.addView(date);
        LinearLayout.LayoutParams tp=lp(-1,-2);tp.setMargins(0,36,0,24);root.addView(glass(time,34),tp);
        TextView hint=label("Добро пожаловать домой",18,true); hint.setGravity(Gravity.CENTER); root.addView(hint,lp(-1,40));
        TextView sub=label("Твоё пространство. Твой ритм.",13,false);sub.setTextColor(muted());sub.setGravity(Gravity.CENTER);root.addView(sub,lp(-1,26));
        View spacer=new View(this);root.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        LinearLayout dock=horizontal();dock.setGravity(Gravity.CENTER);dock.setPadding(8,12,8,12);
        String[] names={"Телефон","Сообщения","Камера","Приложения"};
        String[] icons={"☎","✉","◎","⊞"};
        for(int i=0;i<4;i++){
            final int ix=i; LinearLayout item=vertical();item.setGravity(Gravity.CENTER);
            TextView ic=label(icons[i],26,true);ic.setGravity(Gravity.CENTER);item.addView(ic,lp(58,42));
            TextView nm=label(names[i],10,false);nm.setGravity(Gravity.CENTER);item.addView(nm);
            item.setOnClickListener(v->{if(ix==3)showDrawer();else launchByName(names[ix]);});
            dock.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(glass(dock,28),lp(-1,-2));
        TextView foot=label("Проведи вверх, чтобы открыть приложения",11,false);foot.setTextColor(muted());foot.setGravity(Gravity.CENTER);root.addView(foot,lp(-1,34));
        root.setOnTouchListener((v,e)->{if(e.getAction()==android.view.MotionEvent.ACTION_UP && e.getY()<root.getHeight()*0.8f && e.getY()>root.getHeight()*0.35f){return false;} return false;});
        root.setOnClickListener(v->{});
    }
    void loadApps(){
        Intent q=new Intent(Intent.ACTION_MAIN,null);q.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list=getPackageManager().queryIntentActivities(q,0);
        apps.clear();for(ResolveInfo r:list)if(!r.activityInfo.packageName.equals(getPackageName()))apps.add(r);
        Collections.sort(apps,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
    }
    void showDrawer(){
        drawer=true;root.removeAllViews();
        LinearLayout top=horizontal();TextView back=label("‹",36,true);back.setPadding(8,0,18,0);back.setOnClickListener(v->buildHome());top.addView(back);
        TextView title=label("Приложения",24,true);top.addView(title);root.addView(top,lp(-1,58));
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(ink());search.setHintTextColor(muted());search.setHint("Поиск приложений");search.setPadding(18,8,18,8);search.setBackground(bg(panel(),0x66FFFFFF,24));
        LinearLayout.LayoutParams sp=lp(-1,52);sp.setMargins(0,12,0,18);root.addView(search,sp);
        ScrollView scroll=new ScrollView(this);grid=vertical();scroll.addView(grid);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        renderApps("");
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){renderApps(s.toString());}
            public void afterTextChanged(android.text.Editable e){}
        });
    }
    void renderApps(String filter){
        grid.removeAllViews();LinearLayout row=null;int n=0;
        for(ResolveInfo r:apps){
            String name=r.loadLabel(getPackageManager()).toString();
            if(!name.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)))continue;
            if(n%4==0){row=horizontal();row.setGravity(Gravity.TOP);grid.addView(row,lp(-1,104));}
            LinearLayout item=vertical();item.setGravity(Gravity.CENTER);
            ImageView icon=new ImageView(this);icon.setImageDrawable(r.loadIcon(getPackageManager()));item.addView(icon,lp(48,48));
            TextView title=label(name,11,false);title.setGravity(Gravity.CENTER);title.setMaxLines(2);item.addView(title,lp(-1,38));
            item.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setClassName(r.activityInfo.packageName,r.activityInfo.name);try{startActivity(i);}catch(Exception ignored){}});
            row.addView(item,new LinearLayout.LayoutParams(0,-2,1));n++;
        }
        if(n==0){TextView empty=label("Ничего не найдено",15,false);empty.setGravity(Gravity.CENTER);grid.addView(empty,lp(-1,100));}
    }
    void launchByName(String term){
        Intent q=new Intent(Intent.ACTION_MAIN,null);q.addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo r:getPackageManager().queryIntentActivities(q,0)){
            String n=r.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT);
            if(n.contains(term.toLowerCase(Locale.ROOT))){Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setClassName(r.activityInfo.packageName,r.activityInfo.name);try{startActivity(i);}catch(Exception ignored){}return;}
        }
        showDrawer();
    }
    @Override public void onBackPressed(){if(drawer)buildHome();else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();if(root!=null&&drawer)loadApps();}
    @Override protected void onDestroy(){super.onDestroy();if(ticker!=null)handler.removeCallbacks(ticker);}
}
