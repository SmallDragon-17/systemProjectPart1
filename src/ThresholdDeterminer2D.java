//
//	２次元の特徴量の閾値計算クラスのインターフェース
//
interface  ThresholdDeterminer2D
{
	// 閾値の決定方法の名前を返す
	public String  getThresholdName();
	
	// 両グループの特徴量から閾値を決定
	public void  determine( float[][] features0, float[][] features1 );
	
	// 与えられた特徴量からどちらの文字かを判定
	public int  recognize( float[] feature );
	
	// 閾値を返す
	public float  getThreshold( float f0 );
	
	// 特徴空間のデータをグラフに描画（グラフオブジェクトに図形データを設定）
	public void  drawGraph( GraphViewer gv );
};
