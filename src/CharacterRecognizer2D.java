import java.awt.image.BufferedImage;
//import java.awt.Color;


//
//	文字画像認識クラス（２次元の特徴量に対応した拡張版）
//
class  CharacterRecognizer2D extends CharacterRecognizer
{
	// １次元・２次元のどちらの特徴量を認識に使用するかの設定
	protected int   dimension = 1;
	
	// 特徴量の評価用オブジェクト
	protected FeatureEvaluater    feature_evaluater2;
	
	// 閾値の決定用オブジェクト（２次元の特徴量への対応版）
	protected ThresholdDeterminer2D  threshold_determiner_2d;
	
	// 学習に使用した画像の特徴量（２次元の特徴量）
	protected float  features2d0[][];
	protected float  features2d1[][];
	
	
	// 認識に使用する次元数の設定
	public void  setDimension( int d )
	{
		dimension = d;
	}
	
	// 特徴量の評価用オブジェクトを設定（設定する軸を指定）
	public void  setFeatureEvaluater( int dim, FeatureEvaluater fe )
	{
		if ( dim == 0 )
			super.setFeatureEvaluater( fe );
		else if ( dim == 1 )
			feature_evaluater2 = fe;
	}
	
	// 閾値の計算用オブジェクト（２次元の特徴量への対応版）を設定
	public void  setThresholdDeterminer2D( ThresholdDeterminer2D td )
	{
		threshold_determiner_2d = td;
	}
	
	// 認識に使用する次元数の取得
	public int  getDimension( int d )
	{
		return  dimension;
	}
	
	// 特徴量の評価用オブジェクトを取得（取得する軸を指定）
	public FeatureEvaluater  getFeatureEvaluater( int dim )
	{
		if ( dim == 0 )
			return  super.getFeatureEvaluater();
		else if ( dim == 1 )
			return feature_evaluater2;
		else
			return  null;
	}
	
	// 閾値の計算用オブジェクト（２次元の特徴量への対応版）を取得
	public ThresholdDeterminer2D  getThresholdDeterminer2D()
	{
		return  threshold_determiner_2d;
	}


	// 与えられた２つのグループの画像データを判別するような特徴量の閾値を計算
	public void  train( BufferedImage[] images0, BufferedImage[] images1 )
	{
		// 使用する特徴量が１次元であれば基底クラスの処理を使用
		if ( dimension == 1 )
		{
			super.train( images0, images1 );
			return;
		}
		
		// 使用する特徴量が２次元以外であれば対応していないので何もせず終了
		else if ( dimension != 2 )
			return;
		
		// 計算用オブジェクトが未設定であれば処理は行わずに終了
		if ( ( feature_evaluater == null ) || ( feature_evaluater2 == null ) || ( threshold_determiner_2d == null ) )
			return;
		
		// 各画像の特徴量を計算（２次元）
		features2d0 = new float[ images0.length ][ 2 ];
		features2d1 = new float[ images1.length ][ 2 ];
		
		// １次元目の特徴量を計算
		for ( int i=0; i<images0.length; i++ )
			features2d0[ i ][ 0 ] = feature_evaluater.evaluate( images0[ i ] );
		for ( int i=0; i<images1.length; i++ )
			features2d1[ i ][ 0 ] = feature_evaluater.evaluate( images1[ i ] );

		// ２次元目の特徴量を計算
		for ( int i=0; i<images0.length; i++ )
			features2d0[ i ][ 1 ] = feature_evaluater2.evaluate( images0[ i ] );
		for ( int i=0; i<images1.length; i++ )
			features2d1[ i ][ 1 ] = feature_evaluater2.evaluate( images1[ i ] );

		// 特徴量の分布から２つのグループを識別するような閾値を決定
		threshold_determiner_2d.determine( features2d0, features2d1 );
	}
	
	// 学習結果に基づいて与えられた画像を判別（判別した画像の種類 0 or 1 を返す）
	public int  recognizeCharacter( BufferedImage image )
	{
		// 使用する特徴量が１次元であれば基底クラスの処理を使用
		if ( dimension == 1 )
			return  super.recognizeCharacter( image );
		
		// 使用する特徴量が２次元以外であれば対応していないので何もせず終了
		else if ( dimension != 2 )
			return  -1;
		
		// 計算用オブジェクトが未設定であれば処理は行わずに終了
		if ( ( feature_evaluater == null ) || ( feature_evaluater2 == null ) || ( threshold_determiner_2d == null ) )
			return  -1;
		
		// 与えられた画像の特徴量を計算
		float  feature2d[] = new float[ 2 ];
		feature2d[ 0 ] = feature_evaluater.evaluate( image );
		feature2d[ 1 ] = feature_evaluater2.evaluate( image );
		
		// 与えられた画像を認識
		return  threshold_determiner_2d.recognize( feature2d );
	}

	// 特徴空間のデータを描画（グラフオブジェクトにデータを設定）
	public void  drawGraph( GraphViewer gv )
	{
		// 使用する特徴量が１次元であれば基底クラスの処理を使用
		if ( dimension == 1 )
		{
			super.drawGraph( gv );
			return;
		}
		
		// 使用する特徴量が２次元以外であれば対応していないので何もせず終了
		else if ( dimension != 2 )
			return;
		
		// グラフをクリア
		gv.clearFigure();

		// 特徴空間のデータ・閾値のデータをグラフに設定
		threshold_determiner_2d.drawGraph( gv );
		
		// グラフの描画範囲を自動設定
		gv.setGraphAreaAuto();
	}
}
	
