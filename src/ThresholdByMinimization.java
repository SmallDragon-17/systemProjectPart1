import java.awt.Color;
import java.util.ArrayList;


//
//	誤認識率の和に基づく閾値の計算クラス
//
class  ThresholdByMinimization extends ThresholdByCumulative
{
	// 合計の誤認識率の分布
	float[]  error_sum;


	// 閾値の決定方法の名前を返す
	public String  getThresholdName()
	{
		return  "誤認識率が最小になるように閾値を決定";
	}

	// 両グループの特徴量から閾値を決定する
	public void  determine( float[] features0, float[] features1 )
	{
		// 基底クラス（ThresholdByCumulative）の閾値計算処理を実行（累積分布までを計算）
		super.determine( features0, features1 );

		// 累積分布から合計の誤認識率の分布を計算
		makeErrorSum();


		// 各区間ごとに合計の誤認識率が最大になるかどうかを調べて、最大の区間を閾値とする
		// 要実装

		// 最大値を求めるのみ
		float maxFalseRate = error_sum[0];
		ArrayList<Float> floatList = new ArrayList<Float>();
		for(float iErrorSum: error_sum){
			maxFalseRate = Math.max(maxFalseRate, iErrorSum);
			floatList.add(iErrorSum);
		}

//		threshold = ...;
		float newThreshold = (floatList.indexOf(maxFalseRate) + 0.5f) * histogram_delta_f + histogram_min_f;
		threshold = newThreshold;
	}

	// 特徴空間のデータをグラフに描画（グラフオブジェクトに図形データを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// 合計の誤認識率の分布を折れ線グラフで描画
		drawErrorSum( gv );

		// データ分布を散布図で描画
		drawScatteredGraph( gv, 0.0f, -0.1f );

		// 閾値を描画
		drawThreshold( gv );
	}


	//
	//  閾値計算のための内部メソッド
	//

	// 累積分布から合計の誤認識率の分布を計算
	protected void  makeErrorSum()
	{
		error_sum = new float[ cumulative0.length ];
		for ( int i=0; i<cumulative0.length; i++ )
			error_sum[ i ] = Math.abs( cumulative0[ i ] - cumulative1[ i ] );
	}


	//
	//  特徴空間描画のための内部メソッド
	//

	// 合計の誤認識率の分布を折れ線グラフで描画
	protected void  drawErrorSum( GraphViewer gv )
	{
		// 確率分布グラフを描画
		GraphPoint  data[];
		data = new GraphPoint[ error_sum.length ];
		int  i;
		for ( i=0; i<error_sum.length; i++ )
		{
			data[ i ] = new GraphPoint();
			data[ i ].x = histogram_min_f + histogram_delta_f * ( i + 0.5f );
			data[ i ].y = error_sum[ i ];
		}
		gv.addFigure( GraphViewer.FIG_LINE, Color.BLACK, data );
	}
}


