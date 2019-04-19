import java.awt.image.BufferedImage;
import java.awt.Graphics;


//
//  文字画像の特徴量計算クラスのインターフェース
//
interface  FeatureEvaluater
{
	// 特徴量の名前を返す
	public String  getFeatureName();
	
	// 文字画像から１次元の特徴量を計算する
	public float  evaluate( BufferedImage image );
	
	// 最後に行った特徴量計算の結果を描画する
	public void  paintImageFeature( Graphics g );
};
