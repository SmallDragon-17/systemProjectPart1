//
//	特徴量ごとに異なる正規分布に基づく２次元の特徴量の閾値の計算クラス
//
class  Threshold2DByGaussian2 extends Threshold2DByGaussian1
{
	// 特徴量の分散
	protected float  variance_x, variance_y;


	// 閾値の決定方法の名前を返す
	public String  getThresholdName()
	{
		return  "特徴量ごとに異なる正規分布を仮定";
	}

	// 両グループの特徴量から閾値を決定
	public void  determine( float[][] features0, float[][] features1 )
	{
		// 基底クラスの処理を実行（特徴量の平均値の計算）
				super.determine( features0, features1 );

				// 特徴量の分散を計算
				variance_x = 0.0f;
				variance_y = 0.0f;
				for ( int i=0; i<features0.length; i++ )
				{
					float  dx = features0[ i ][ 0 ] - mean0x;
					float  dy = features0[ i ][ 1 ] - mean0y;
					variance_x += dx * dx / ( features0.length + features1.length );
					variance_y += dy * dy / ( features0.length + features1.length );
				}
				for ( int i=0; i<features1.length; i++ )
				{
					float  dx = features1[ i ][ 0 ] - mean1x;
					float  dy = features1[ i ][ 1 ] - mean1y;
					variance_x += dx * dx / ( features0.length + features1.length );
					variance_y += dy * dy / ( features0.length + features1.length );
				}

				// 境界の方程式の傾きを修正
				//（２つの平均値からの距離を分散で割ったものが等しくなる直線を境界とする）
				border_dy = - ( ( mean1x - mean0x ) * variance_y ) / ( ( mean1y - mean0y ) * variance_x );
	}
}
