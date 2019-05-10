import java.awt.Color;


//
//  同一の正規分布に基づく２次元の特徴量の閾値の計算クラス
//
class  Threshold2DByGaussian1 implements ThresholdDeterminer2D
{
	// 特徴量の平均値
	protected float  mean0x, mean0y;
	protected float  mean1x, mean1y;

	// 閾値（境界）の方程式
	protected float  border_ox, border_oy; // 境界線の中心点
	protected float  border_dy; // 境界線の傾き

	// 閾値の符号（グループ0の方が特徴量が閾値よりも小さければ真）
	protected boolean  is_first_smaller;

	// 特徴量データ（グラフ描画用）
	protected float  features0[][];
	protected float  features1[][];


	// 閾値の決定方法の名前を返す
	public String  getThresholdName()
	{
		return  "同一の正規分布を仮定";
	}

	// 両グループの特徴量から閾値を決定
	public void  determine( float[][] features0, float[][] features1 )
	{
		// 特徴量の平均値を計算
				int  i;
				mean0x = 0.0f;
				mean0y = 0.0f;
				for ( i=0; i<features0.length; i++ )
				{
					mean0x += features0[ i ][ 0 ];
					mean0y += features0[ i ][ 1 ];
				}
				mean0x = mean0x / features0.length;
				mean0y = mean0y / features0.length;

				mean1x = 0.0f;
				mean1y = 0.0f;
				for ( i=0; i<features1.length; i++ )
				{
					mean1x += features1[ i ][ 0 ];
					mean1y += features1[ i ][ 1 ];
				}
				mean1x = mean1x / features1.length;
				mean1y = mean1y / features1.length;

				// 境界の方程式を計算（２つの平均値からの距離が等しくなる直線を境界とする）
				border_ox = ( mean0x + mean1x ) * 0.5f;
				border_oy = ( mean0y + mean1y ) * 0.5f;
				border_dy = - ( mean1x - mean0x ) / ( mean1y - mean0y );

				// 境界の符号を判定（グループ0が特徴量が境界よりも下にあれば真）
				if ( mean0y < getThreshold( mean0x ) )
					is_first_smaller = true;
				else
					is_first_smaller = false;

				// 特徴量データを記録（グラフ描画用）
				this.features0 = features0;
				this.features1 = features1;
	}

	// 閾値をもとに特徴量から文字を判定する
	public int  recognize( float[] feature )
	{
		// グループ0の特徴量y < 閾値y < グループ1の特徴量y
		if ( is_first_smaller )
		{
			if ( feature[ 1 ] < getThreshold( feature[ 0 ] ) )
				return  0;
			else
				return  1;
		}
		// グループ1の特徴量y < 閾値 < グループ0の特徴量y
		else
		{
			if ( feature[ 1 ] < getThreshold( feature[ 0 ] ) )
				return  1;
			else
				return  0;
		}
	}

	// 閾値を返す
	public float  getThreshold( float x )
	{
		float  y;
		y = ( x - border_ox ) * border_dy + border_oy;
		return  y;
	}

	// 特徴空間のデータをグラフに描画（グラフオブジェクトに図形データを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// データ分布を散布図で描画
		drawScatteredGraph( gv );

		// 境界線を描画
		drawBorderLine( gv );
	}


	//
	//  特徴空間描画のための内部メソッド
	//

	// 境界線を描画
	protected void  drawBorderLine( GraphViewer gv )
	{
		// 特徴量の範囲を調べる
		float  min_x, max_x, min_y, max_y;
		max_x = min_x = features0[ 0 ][ 0 ];
		max_y = min_y = features0[ 0 ][ 1 ];
		for ( int i=1; i<features0.length; i++ )
		{
			float  fx = features0[ i ][ 0 ];
			float  fy = features0[ i ][ 1 ];
			if ( min_x > fx )
				min_x = fx;
			else if ( max_x < fx )
				max_x = fx;
			if ( min_y > fy )
				min_y = fy;
			else if ( max_y < fy )
				max_y = fy;
		}
		for ( int i=0; i<features1.length; i++ )
		{
			float  fx = features1[ i ][ 0 ];
			float  fy = features1[ i ][ 1 ];
			if ( min_x > fx )
				min_x = fx;
			else if ( max_x < fx )
				max_x = fx;
			if ( min_y > fy )
				min_y = fy;
			else if ( max_y < fy )
				max_y = fy;
		}

		// 直線の両端点を結ぶ線分を計算
		float  x0, y0, x1, y1;
		x0 = min_x;
		y0 = getThreshold( min_x );
		x1 = max_x;
		y1 = getThreshold( max_x );
		if ( ( y0 < min_y ) || ( y0 > max_y ) )
		{
			if ( y0 < min_y )
				y0 = min_y;
			else
				y0 = max_y;
			x0 = ( y0 - border_oy ) / border_dy + border_ox;
		}
		if ( ( y1 < min_y ) || ( y1 > max_y ) )
		{
			if ( y1 < min_y )
				y1 = min_y;
			else
				y1 = max_y;
			x1 = ( y1 - border_oy ) / border_dy + border_ox;
		}

		// 線分を描画
		GraphPoint  data[];
		data = new GraphPoint[ 2 ];
		data[ 0 ] = new GraphPoint();
		data[ 0 ].x = x0;
		data[ 0 ].y = y0;
		data[ 1 ] = new GraphPoint();
		data[ 1 ].x = x1;
		data[ 1 ].y = y1;
		gv.addFigure( GraphViewer.FIG_LINE, Color.BLACK, data );
	}

	// データ分布を散布図で描画
	protected void  drawScatteredGraph( GraphViewer gv )
	{
		// 両グループの特徴量の平均値を描画
		GraphPoint  data[];
		data = new GraphPoint[ 1 ];
		data[ 0 ] = new GraphPoint();
		data[ 0 ].x = mean0x;
		data[ 0 ].y = mean0y;
		gv.addFigure( GraphViewer.FIG_SCATTERED, new Color( 1.0f, 0.5f, 0.5f ), data, 6.0f );

		data = new GraphPoint[ 1 ];
		data[ 0 ] = new GraphPoint();
		data[ 0 ].x = mean1x;
		data[ 0 ].y = mean1y;
		gv.addFigure( GraphViewer.FIG_SCATTERED, new Color( 0.5f, 0.5f, 1.0f ), data, 6.0f );

		// 各データを散布図で描画
		data = new GraphPoint[ features0.length ];
		for ( int i=0; i<features0.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = features0[ i ][ 0 ];
			data[ i ].y = features0[ i ][ 1 ];
		}
		gv.addFigure( GraphViewer.FIG_SCATTERED, Color.RED, data );

		data = new GraphPoint[ features1.length ];
		for ( int i=0; i<features1.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = features1[ i ][ 0 ];
			data[ i ].y = features1[ i ][ 1 ];
		}
		gv.addFigure( GraphViewer.FIG_SCATTERED, Color.BLUE, data );
	}
}