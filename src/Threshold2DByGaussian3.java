//
//	特徴量ごとに異なる正規分布に基づく２次元の特徴量の閾値の計算クラス
//
class  Threshold2DByGaussian3 extends Threshold2DByGaussian1
{
	// 分散共分散行列
	protected float  C0xx, C0xy, C0yy;
	protected float  C1xx, C1xy, C1yy;

	// 分散共分散の逆行列
	protected float  S0xx, S0xy, S0yy;
	protected float  S1xx, S1xy, S1yy;

	// 前回計算した閾値の値（解が２つ出るので両方の値を記録）
	protected float  last_y0, last_y1;


	// 閾値の決定方法の名前を返す
	public String  getThresholdName()
	{
		return  "一般の正規分布を仮定";
	}

	// 両グループの特徴量から閾値を決定
	public void  determine( float[][] features0, float[][] features1 )
	{
		// 基底クラスの処理を実行（特徴量の平均値の計算）
		super.determine( features0, features1 );

		// 特徴量の分散共分散を計算
		C0xx = C0xy = C0yy = 0.0f;
		for ( int i=0; i<features0.length; i++ )
		{
			float  dx = features0[ i ][ 0 ] - mean0x;
			float  dy = features0[ i ][ 1 ] - mean0y;
			C0xx += dx * dx;
			C0yy += dy * dy;
			C0xy += dx * dy;
		}
		C0xx = C0xx / features0.length;
		C0yy = C0yy / features0.length;
		C0xy = C0xy / features0.length;

		C1xx = C1xy = C1yy = 0.0f;
		for ( int i=0; i<features1.length; i++ )
		{
			float  dx = features1[ i ][ 0 ] - mean1x;
			float  dy = features1[ i ][ 1 ] - mean1y;
			C1xx += dx * dx;
			C1yy += dy * dy;
			C1xy += dx * dy;
		}
		C1xx = C1xx / features1.length;
		C1yy = C1yy / features1.length;
		C1xy = C1xy / features1.length;

		// 分散共分散の逆行列を計算
		float  det;
		det = C0xx * C0yy - C0xy * C0xy;
		S0xx = C0yy / det;
		S0yy = C0xx / det;
		S0xy = - C0xy / det;
		det = C1xx * C1yy - C1xy * C1xy;
		S1xx = C1yy / det;
		S1yy = C1xx / det;
		S1xy = - C1xy / det;
	}

	// 閾値をもとに特徴量から文字を判定する
	public int  recognize( float[] feature )
	{
		// 各グループの平均値からのマハラノビス距離から、どちらの文字かを判定
		float  dist0, dist1;
		float  dx, dy;
		dx = feature[ 0 ] - mean0x;
		dy = feature[ 1 ] - mean0y;
		dist0 = dx * ( S0xx * dx + S0xy * dy ) + dy * ( S0xy * dx + S0yy * dy );
		dx = feature[ 0 ] - mean1x;
		dy = feature[ 1 ] - mean1y;
		dist1 = dx * ( S1xx * dx + S1xy * dy ) + dy * ( S1xy * dx + S1yy * dy );
		if ( dist0 < dist1 )
			return  0;
		else
			return  1;
	}

	// 閾値を返す（特徴量Xの値が与えられたときの特徴量Yの値の閾値を返す）
	public float  getThreshold( float x )
	{
		// ２次方程式の各項を計算（a x^2 + b x + c = 0 ）
		float  m0x = mean0x, m0y = mean0y, m1x = mean1x, m1y = mean1y;
		float  dx0, dx1;
		float  L0 = 0.0f, L1 = 0.0f;
		float  a, b, c;
		dx0 = x - m0x;
		dx1 = x - m1x;
		L0 = 0.5f * (float) java.lang.Math.log( S0xx * S0yy - S0xy * S0xy );
		L1 = 0.5f * (float) java.lang.Math.log( S1xx * S1yy - S1xy * S1xy );
		a = S0yy - S1yy;
		b = 2 * ( - dx0 * S0xy - m0y * S0yy - dx1 * S1xy + m1y * S1yy );
		c = ( -L0 + L1 + dx0 * dx0 * S0xx + 2.0f * dx0 * m0y * S0xy + m0y * m0y * S0yy
		               - dx1 * dx1 * S1xx - 2.0f * dx1 * m1y * S1xy - m1y * m1y * S1yy );

		// 実数解を持つかどうかを判定（２次方程式の解の公式のルート内が正になるかを判定）
		float  root = b * b - 4.0f * a * c;
		if ( root < 0.0f )
		{
			last_y0 = Float.NaN;
			last_y1 = Float.NaN;
			return  Float.NaN; // 非数（無効な実数）を返す
		}
		root = (float) java.lang.Math.sqrt( root );

		// ２次方程式の解を計算
		float  y = 0.0f, y0, y1;
		y0 = ( - b + root ) / ( 2.0f * a );
		y1 = ( - b - root ) / ( 2.0f * a );
		last_y0 = y0;
		last_y1 = y1;

		// ２つの解のうち近い方を仮に返す
		if ( ( ( y0 - m0y ) * ( y0 - m0y ) + ( y0 - m1y ) * ( y0 - m1y ) ) <
		     ( ( y1 - m0y ) * ( y1 - m0y ) + ( y1 - m1y ) * ( y1 - m1y ) ) )
			y = y0;
		else
			y = y1;
		return  y;
	}

	// 特徴空間のデータをグラフに描画（グラフオブジェクトに図形データを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// データ分布を散布図で描画
		drawScatteredGraph( gv );

		// 境界曲線を描画
		drawBorderCurve( gv );
	}


	//
	//  特徴空間描画のための内部メソッド
	//

	// 境界曲線を描画
	protected void  drawBorderCurve( GraphViewer gv )
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

		// いくつかの点の座標を記録（範囲内でXを変化させながら上側のYと下側のYをそれぞれ記録）
		int  num_points = 80;
		GraphPoint  upper_points[];
		GraphPoint  lower_points[];
		upper_points = new GraphPoint[ num_points ];
		lower_points = new GraphPoint[ num_points ];
		int  upper_count = 0;
		int  lower_count = 0;
		float  x, y;
		for ( int i=0; i<num_points; i++ )
		{
			x = ( max_x - min_x ) * i / ( num_points - 1 ) + min_x;
			y = getThreshold( x );

			if ( ! Float.isNaN( last_y0 ) )
			{
				upper_points[ upper_count ] = new GraphPoint();
				upper_points[ upper_count ].x = x;
				upper_points[ upper_count ].y = last_y0;
				upper_count ++;
			}
			if ( ! Float.isNaN( last_y1 ) )
			{
				lower_points[ lower_count ] = new GraphPoint();
				lower_points[ lower_count ].x = x;
				lower_points[ lower_count ].y = last_y1;
				lower_count ++;
			}
		}

		// 取得した点をつなげて折れ線として曲線を描画
		if ( Float.isNaN( getThreshold( min_x ) ) )
		{
			// グラフの右端から始まり、右端に消えるような曲線
			GraphPoint  all_points[] = new GraphPoint[ upper_count + lower_count ];
			for ( int i=0; i<upper_count; i++ )
				all_points[ i ] = upper_points[ upper_count - i - 1 ];
			for ( int i=0; i<lower_count; i++ )
				all_points[ upper_count + i ] = lower_points[ i ];
			gv.addFigure( GraphViewer.FIG_LINE, java.awt.Color.BLACK, all_points );
		}
		else if ( Float.isNaN( getThreshold( max_x ) ) )
		{
			// グラフの左端から始まり、左端に消えるような曲線
			GraphPoint  all_points[] = new GraphPoint[ upper_count + lower_count ];
			for ( int i=0; i<upper_count; i++ )
				all_points[ i ] = upper_points[ i ];
			for ( int i=0; i<lower_count; i++ )
				all_points[ upper_count + i ] = lower_points[ lower_count - 1 - i ];
			gv.addFigure( GraphViewer.FIG_LINE, java.awt.Color.BLACK, all_points );
		}
		else if ( ( upper_count == num_points ) && ( lower_count == num_points ) )
		{
			// グラフの左端から始まり、右端に消えるような曲線
			getThreshold( mean0x );
			if ( java.lang.Math.abs( mean0y - last_y0 ) < java.lang.Math.abs( mean0y - last_y1 ) )
			{
				GraphPoint  all_points[] = new GraphPoint[ upper_count ];
				for ( int i=0; i<upper_count; i++ )
					all_points[ i ] = upper_points[ i ];
				gv.addFigure( GraphViewer.FIG_LINE, java.awt.Color.BLACK, all_points );
			}
			else
			{
				GraphPoint  all_points[] = new GraphPoint[ lower_count ];
				for ( int i=0; i<lower_count; i++ )
					all_points[ i ] = lower_points[ i ];
				gv.addFigure( GraphViewer.FIG_LINE, java.awt.Color.BLACK, all_points );
			}
		}
		else
		{
			// グラフの左端と右端に２つの曲線ができる場合（未対応）
		}
	}
}