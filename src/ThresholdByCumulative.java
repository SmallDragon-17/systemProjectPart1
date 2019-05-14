import java.awt.Color;


//
//	累積分布に基づく閾値の計算クラス
//
class  ThresholdByCumulative extends ThresholdByProbability
{
	// 累積分布
	float[]  cumulative0;
	float[]  cumulative1;


	// 閾値の決定方法の名前を返す
	public String  getThresholdName()
	{
		return  "誤認識率が等しくなるように閾値を決定";
	}

	// 両グループの特徴量から閾値を決定する
	public void  determine( float[] features0, float[] features1 )
	{
		// 基底クラス（ThresholdByAverage）の閾値計算処理を実行（確率分布までを計算）
		super.determine( features0, features1 );

		// 確率分布から累積分布を計算
		makeCumulative();


		// 各区間ごとに累積分布の和が 1.0 になる点があるかどうかを調べる
		float  new_threshold;
		for ( int seg_no=0; seg_no<histogram0.length-1; seg_no++ )
		{
			// 区間の右端・左端の累積分布を調べる
			// 要実装
			float leftCumu = cumulative0[seg_no] + cumulative1[seg_no];
			float rightCumu = cumulative0[seg_no + 1] + cumulative1[seg_no + 1];

			float leftY , rightY;
			leftY = (seg_no + 0.5f) * histogram_delta_f + histogram_min_f;
			rightY = (seg_no + 1 + 0.5f) * histogram_delta_f + histogram_min_f;

			// 左端の累積分布が1.0より小さく、右端の累積分布が1.0以上であれば、
			// その区間で必ず累積分布の和が1.0になる点が存在する
//			if ( /* 要実装 */ )
			if (leftCumu < 1.0 && rightCumu >= 1.0) {
				// 区間内の出現確率が等しい点を計算する
				// 要実装
				new_threshold = leftY + (1 - leftCumu) * (rightY - leftY) / (rightCumu - leftCumu);
				threshold = new_threshold;
			}
		}
	}

	// 特徴空間のデータをグラフに描画（グラフオブジェクトに図形データを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// 累積分布を折れ線グラフで描画
		drawCumulative( gv );

		// データ分布を散布図で描画
		drawScatteredGraph( gv, 0.0f, -0.1f );

		// 閾値を描画
		drawThreshold( gv );
	}


	//
	//  閾値計算のための内部メソッド
	//

	// 確率分布から累積分布を計算
	protected void  makeCumulative()
	{
		cumulative0 = new float[ probability0.length ];
		cumulative0[ 0 ] = probability0[ 0 ];
		for ( int i=1; i<cumulative0.length; i++ )
			cumulative0[ i ] = cumulative0[ i-1 ] + probability0[ i ];

		cumulative1 = new float[ probability1.length ];
		cumulative1[ 0 ] = probability1[ 0 ];
		for ( int i=1; i<cumulative1.length; i++ )
			cumulative1[ i ] = cumulative1[ i-1 ] + probability1[ i ];
	}


	//
	//  特徴空間描画のための内部メソッド
	//

	// 累積分布を折れ線グラフで描画
	protected void  drawCumulative( GraphViewer gv )
	{
		// 確率分布グラフを描画
		GraphPoint  data[];
		data = new GraphPoint[ histogram0.length ];
		int  i;
		for ( i=0; i<histogram0.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = histogram_min_f + histogram_delta_f * ( i + 0.5f );
			data[ i ].y = cumulative0[ i ];
		}
		gv.addFigure( GraphViewer.FIG_LINE, Color.RED, data );

		data = new GraphPoint[ histogram1.length ];
		for ( i=0; i<histogram1.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = histogram_min_f + histogram_delta_f * ( i + 0.5f );
			data[ i ].y = cumulative1[ i ];
		}
		gv.addFigure( GraphViewer.FIG_LINE, Color.BLUE, data );
	}
}
