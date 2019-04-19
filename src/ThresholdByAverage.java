import java.awt.Color;


//
//	平均値に基づく閾値の計算クラス
//
class  ThresholdByAverage implements ThresholdDeterminer
{
	// 閾値と符号（グループ0の方が特徴量が閾値よりも小さければ真）
	protected float  threshold;
	protected boolean  is_first_smaller;
	
	// 特徴量データ（グラフ描画用）
	protected float  features0[];
	protected float  features1[];
	
	// 各グループの特徴量の平均値（グラフ描画用）
	protected float  average0;
	protected float  average1;
	
	// 度数分布（ヒストグラム）
	protected float  histogram_min_f, histogram_max_f, histogram_delta_f;
	protected int[]  histogram0;
	protected int[]  histogram1;
	
	// 度数分布のデフォルトの区間幅
	float  default_histogram_delta = 0.02f;
	int    default_histogram_size = 20;
	
	
	// 閾値の決定方法の名前を返す
	public String  getThresholdName()
	{
		return  "平均値による閾値の決定";
	}
	
	// 両グループの特徴量から閾値を決定する
	public void  determine( float[] features0, float[] features1 )
	{
		// 各グループの平均値を計算
		average0 = 0.0f; // 要実装
		average1 = 0.0f; // 要実装
		
		// ２つの平均値の中央値を計算
		threshold = 0.0f; // 要実装
		
		// 符号を計算
		is_first_smaller = false; // 要実装
		
		// 特徴量データを記録（グラフ描画用）
		this.features0 = features0;
		this.features1 = features1;
	}

	// 閾値をもとに特徴量から文字を判定する
	public int  recognize( float feature )
	{
		// グループ0の特徴量 < 閾値 < グループ1の特徴量
		if ( is_first_smaller )
		{
			if ( feature < threshold ) 
				return  0;
			else
				return  1;
		}
		// グループ1の特徴量 < 閾値 < グループ0の特徴量
		else
		{
			if ( feature < threshold ) 
				return  1;
			else
				return  0;
		}
	}
	
	// 閾値を返す
	public float  getThreshold()
	{
		return  threshold;
	}
	
	// 特徴空間のデータをグラフに描画（グラフオブジェクトに図形データを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// ２つの特徴量の度数分布（ヒストグラム）を計算
		//（ヒストグラムを作成する方法として２通り用意されているので、どちらか適当な方を呼び出す。）
//		makeHistogramsByWidth( default_histogram_delta );
		makeHistogramsBySize( default_histogram_size );
		
		// データ分布を散布図で描画
		drawScatteredGraph( gv, 0.0f, -1.0f );
		
		// 度数分布を棒グラフで描画
		drawHistogram( gv );
		
		// 平均値を縦線で描画
		drawAverage( gv );
		
		// 閾値を描画
		drawThreshold( gv );
	}
	
	
	//
	//  閾値計算のための内部メソッド
	//
	
	// 指定された範囲・区間で特徴量の度数分布（ヒストグラム）を計算
	//（min_f, maxf はヒストグラムを作成する特徴量の範囲、delta_f は各区間ごとの特徴量の幅）
	protected int[]  makeHistogram( float[] features, float min_f, float max_f, float delta_f )
	{
		// ヒストグラムの区間数を計算して配列を初期化
		int  histogram_size = (int) java.lang.Math.ceil( ( max_f - min_f ) / delta_f );
		int[]  histogram = new int[ histogram_size ];
		
		// ヒストグラムの各区間におけるデータの出現回数をカウント
		for ( int i=0; i<features.length; i++ )
		{
			int  seg_no = (int) java.lang.Math.floor( ( features[ i ] - min_f ) / delta_f );
			if ( seg_no <= 0 )
				seg_no = 1;
			if ( seg_no >= histogram_size - 1 )
				seg_no = histogram_size - 2;
			histogram[ seg_no ] ++;
		}
		
		return  histogram;
	}
	
	// 特徴量の度数分布（ヒストグラム）を計算
	//（全体をいくつの区間に分けるかという区間数を指定、区間幅は自動決定）
	protected void  makeHistogramsBySize( int segment_size )
	{
		// 度数を調べる範囲を設定
		float  delta_f;
		float  min_f, max_f;
		min_f = features0[ 0 ];
		max_f = features0[ 0 ];
		int  i;
		for ( i=1; i<features0.length; i++ )
		{
			if ( features0[ i ] < min_f )
				min_f = features0[ i ];
			if ( features0[ i ] > max_f )
				max_f = features0[ i ];
		}
		for ( i=0; i<features1.length; i++ )
		{
			if ( features1[ i ] < min_f )
				min_f = features1[ i ];
			if ( features1[ i ] > max_f )
				max_f = features1[ i ];
		}
		
		// 範囲・区間数に応じて区間幅を設定
		delta_f = ( max_f - min_f ) / segment_size;
		
		// 範囲・区間幅が０であれば問題が出るので、仮に設定
		if ( max_f == min_f )
		{
			delta_f = 0.01f / segment_size; // 適当
			max_f = min_f + delta_f * segment_size;
		}
		
		// ヒストグラムの設定を記録
		histogram_min_f = min_f;
		histogram_max_f = max_f;
		histogram_delta_f = delta_f;
		
		// 両端の区間の分布が０になるように、左右に区間を１つ追加する
		histogram_min_f -= delta_f;
		histogram_max_f += delta_f;
		
		// ヒストグラムを作成
		histogram0 = makeHistogram( features0, histogram_min_f, histogram_max_f, delta_f );
		histogram1 = makeHistogram( features1, histogram_min_f, histogram_max_f, delta_f );
	}
	
	// 特徴量の度数分布（ヒストグラム）を計算
	//（各区間の区間幅を指定、区間数は自動決定）
	protected void  makeHistogramsByWidth( float delta_f )
	{
		// 度数を調べる範囲を設定
		float  min_f, max_f;
		min_f = features0[ 0 ];
		max_f = features0[ 0 ];
		int  i;
		for ( i=1; i<features0.length; i++ )
		{
			if ( features0[ i ] < min_f )
				min_f = features0[ i ];
			if ( features0[ i ] > max_f )
				max_f = features0[ i ];
		}
		for ( i=0; i<features1.length; i++ )
		{
			if ( features1[ i ] < min_f )
				min_f = features1[ i ];
			if ( features1[ i ] > max_f )
				max_f = features1[ i ];
		}
		
		// 区間幅に応じて範囲を調整（区間の両端値が区間幅の整数倍になるよう調整）
		min_f = (int)( min_f / delta_f ) * delta_f;
		max_f = (int)( max_f / delta_f + 1 ) * delta_f;
		
		// ヒストグラムの設定を記録
		histogram_min_f = min_f;
		histogram_max_f = max_f;
		histogram_delta_f = delta_f;
		
		// 両端の区間の分布が０になるように、左右に区間を１つ追加する
		histogram_min_f -= delta_f;
		histogram_max_f += delta_f;
		
		// ヒストグラムを作成
		histogram0 = makeHistogram( features0, histogram_min_f, histogram_max_f, delta_f );
		histogram1 = makeHistogram( features1, histogram_min_f, histogram_max_f, delta_f );
	}


	//
	//  特徴空間描画のための内部メソッド
	//
	
	// 閾値を描画
	protected void  drawThreshold( GraphViewer gv )
	{
		// 閾値を描画
		GraphPoint  data[];
		data = new GraphPoint[ 1 ];
		data[ 0 ] = new GraphPoint();
		data[ 0 ].x = getThreshold();
		data[ 0 ].y = 0.0f;
//		gv.addFigure( GraphViewer.FIG_Y_LINE, Color.GREEN, data );
		gv.addFigure( GraphViewer.FIG_Y_LINE, Color.MAGENTA, data );
	}
	
	// データ分布を散布図で描画
	protected void  drawScatteredGraph( GraphViewer gv )
	{
		drawScatteredGraph( gv, 0.0f, 0.0f );
	}
	
	// データ分布を散布図で描画
	protected void  drawScatteredGraph( GraphViewer gv, float y0, float y1 )
	{
		// 各データを散布図で描画
		//（特徴量を x座標として、各サンプルを点で描画）
		//（y座標は、指定された範囲 y0～y1 の間に、順番に並べる）
		//（同じ y座標に描画すると、点が重なって見にくいので、範囲を指定して、少しずつずらしながら描画）
		GraphPoint  data[];
		data = new GraphPoint[ features0.length ];
		for ( int i=0; i<features0.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = features0[ i ];
			data[ i ].y = ( y1 - y0 ) * ( (float)i / features0.length ) + y0;
		}
		gv.addFigure( GraphViewer.FIG_SCATTERED, Color.RED, data );
		
		data = new GraphPoint[ features1.length ];
		for ( int i=0; i<features1.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = features1[ i ];
			data[ i ].y = ( y1 - y0 ) * ( (float)i / features1.length ) + y0;
		}
		gv.addFigure( GraphViewer.FIG_SCATTERED, Color.BLUE, data );
	}
	
	// 平均値を縦線で描画
	protected void  drawAverage( GraphViewer gv )
	{
		// 両グループの特徴量の平均値を描画
		GraphPoint  data[];
		data = new GraphPoint[ 1 ];
		data[ 0 ] = new GraphPoint();
		data[ 0 ].x = average0;
		data[ 0 ].y = 0.0f;
		gv.addFigure( GraphViewer.FIG_Y_LINE, new Color( 1.0f, 0.5f, 0.5f ), data );
		
		data = new GraphPoint[ 1 ];
		data[ 0 ] = new GraphPoint();
		data[ 0 ].x = average1;
		data[ 0 ].y = 0.0f;
		gv.addFigure( GraphViewer.FIG_Y_LINE, new Color( 0.5f, 0.5f, 1.0f ), data );
	}

	// 度数分布を棒グラフで描画
	protected void  drawHistogram( GraphViewer gv )
	{
		// 度数分布グラフを描画
		GraphPoint  data[];
		data = new GraphPoint[ histogram0.length ];
		int  i;
		for ( i=0; i<histogram0.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = histogram_min_f + histogram_delta_f * ( i + 0.5f - 0.1f ); // 少しずらす
			data[ i ].y = histogram0[ i ];
		}
		gv.addFigure( GraphViewer.FIG_BAR, Color.RED, data, default_histogram_delta * 0.8f );

		data = new GraphPoint[ histogram1.length ];
		for ( i=0; i<histogram1.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = histogram_min_f + histogram_delta_f * ( i + 0.5f + 0.1f ); // 少しずらす
			data[ i ].y = histogram1[ i ];
		}
		gv.addFigure( GraphViewer.FIG_BAR, Color.BLUE, data, default_histogram_delta * 0.8f );
	}
};
