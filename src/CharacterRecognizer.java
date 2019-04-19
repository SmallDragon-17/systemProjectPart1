import java.awt.image.BufferedImage;
//import java.awt.Color;


//
//  文字画像認識クラス
//
class  CharacterRecognizer
{
	// 特徴量の評価用オブジェクト
	protected FeatureEvaluater    feature_evaluater;
	
	// 閾値の決定用オブジェクト
	protected ThresholdDeterminer  threshold_determiner;
	
	// 学習に使用した画像の特徴量
	protected float  features0[];
	protected float  features1[];
	
	
	// 特徴量の評価用オブジェクトを設定
	public void  setFeatureEvaluater( FeatureEvaluater fe )
	{
		feature_evaluater = fe;
	}
	
	// 閾値の計算用オブジェクトを設定
	public void  setThresholdDeterminer( ThresholdDeterminer td )
	{
		threshold_determiner = td;
	}
	
	// 特徴量の評価用オブジェクトを取得
	public FeatureEvaluater  getFeatureEvaluater()
	{
		return  feature_evaluater;
	}
	
	// 閾値の計算用オブジェクトを取得
	public ThresholdDeterminer  getThresholdDeterminer()
	{
		return  threshold_determiner;
	}
	
	
	// 与えられた２つのグループの画像データを判別するような特徴量の閾値を計算
	public void  train( BufferedImage[] images0, BufferedImage[] images1 )
	{
		// 計算用オブジェクトが未設定であれば処理は行わない
		if ( ( feature_evaluater == null ) || ( threshold_determiner == null ) )
			return;
		
		// 各画像の特徴量を計算
		features0 = new float[ images0.length ];
		features1 = new float[ images1.length ];
		for ( int i=0; i<images0.length; i++ )
		{
			if ( images0[ i ] != null )
				features0[ i ] = feature_evaluater.evaluate( images0[ i ] );
		}
		for ( int i=0; i<images1.length; i++ )
		{
			if ( images1[ i ] != null )
				features1[ i ] = feature_evaluater.evaluate( images1[ i ] );
		}

		// 特徴量の分布から２つのグループを識別するような閾値を決定
		threshold_determiner.determine( features0, features1 );
	}
	
	// 学習結果に基づいて与えられた画像を判別（判別した画像の種類 0 or 1 を返す）
	public int  recognizeCharacter( BufferedImage image )
	{
		// 計算用オブジェクトが未設定であれば処理は行わない
		if ( ( feature_evaluater == null ) || ( threshold_determiner == null ) )
			return  -1;
		if ( image == null )
			return  -1;
		
		// 与えられた画像の特徴量を計算
		float  feature = feature_evaluater.evaluate( image );
		
		// 与えられた画像を認識
		return  threshold_determiner.recognize( feature );
	}

	
	//
	//  グラフ描画
	//
	
	// 特徴空間のデータを描画（グラフオブジェクトにデータを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// グラフをクリア
		gv.clearFigure();

		// 特徴空間のデータ・閾値のデータをグラフに設定
		threshold_determiner.drawGraph( gv );
		
		// グラフの描画範囲を自動設定
		gv.setGraphAreaAuto();
	}
}