// <applet code="RecognitionApp.class" width="480" height="480"></applet>

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


//
//  文字画像認識テスト アプレット
//
public class RecognitionApp extends JApplet
{
	// 文字画像認識モジュール
	protected CharacterRecognizer2D  recognizer;

	// 利用可能な特徴量計算モジュール
	protected FeatureEvaluater   features[];

	// 利用可能な閾値決定モジュール（１次元の特徴量）
	protected ThresholdDeterminer  thresholds[];

	// 利用可能な閾値決定モジュール（２次元の特徴量）
	protected ThresholdDeterminer2D  thresholds2d[];

	// グラフ描画モジュール
	protected GraphViewer  graph_viewr;

	// 実行モード
	protected int  mode;
	protected final int  RECOGNITION_MODE = 1;
	protected final int  RECOGNITION_2D_MODE = 3;
	protected final int  FEATURE_MODE = 2;

	// 学習・文字認識テストの結果（誤認識率）
	protected float  error, error0, error1;


	// 読み込む画像の設定
	protected String   image_dir = "Samplesれわ/"; // フォルダ名（プロジェクトからの相対パス）
	protected String   image_ext = ".bmp";          // 拡張子
	protected String   image_name0 = "picれ_";       // 頭につける文字列(文字0)
	protected String   image_name1 = "picわ_";       // 頭につける文字列(文字1)
	protected int      num_images0 = 100;            // ファイル数(文字0)
	protected int      num_images1 = 100;            // ファイル数(文字1)
	protected int      image_digits = 3;            // 連番の桁数
	protected String   character0 = "れ";            // 文字名(表示用)(文字0)
	protected String   character1 = "わ";            // 文字名(表示用)(文字0)


	//　画像設定の追加
	// 学習用データセット
	protected BufferedImage training_images0[];
	protected BufferedImage training_images1[];

	// 検証用データセット
	protected BufferedImage evaluation_images0[];
	protected BufferedImage evaluation_images1[];

	// サンプル画像
	protected BufferedImage  sample_images0[];
	protected BufferedImage  sample_images1[];

	// 全サンプル画像のインデックス（画像名→画像オブジェクト の参照）
	protected TreeMap  image_index;


	// UI用コンポーネント
	protected JPanel  ui_panel;
	protected JLabel  mode_button_label, threshold_label, feature_label[], image_label;
	protected JRadioButton  mode_r_bottun, mode_f_bottun;
	protected JComboBox  threshold_list, feature_list[], image_list;
	protected JRadioButton  mode_r2_bottun;
	protected JLabel  threshold2_label;
	protected JComboBox  threshold2_list;
	protected MainScreen  screen;

	// エラーメッセージ
	protected String  error_message;

	// 学習用・評価用画像の決定方法の設定
	enum  DistributionMethod
	{
		USE_ALL_SAMPLES,
		CROSS_VALIDATION,
//		BOOTSTRAP
	};

	// 学習用・評価用画像の決定方法
	protected DistributionMethod  distribution_method =
			DistributionMethod.CROSS_VALIDATION;

	// 学習・評価用設定
	// Cross Validation 法を用いるときのグループ数
	protected int  cv_number_of_folds = 10;

	// Cross Validation 法を用いるとき、何番目のグループを評価に使用するか設定
	protected int  cv_evaluation_fold = 0;


	// 初期化処理
	public void  init()
	{
		// 利用可能な特徴量計算モジュールを初期化
		features = new FeatureEvaluater[ 2 ];
		features[ 0 ] = new FeatureLeftLinerity();
		features[ 1 ] = new FeatureWidthRatio2();

		// 利用可能な閾値決定モジュール（１次元の特徴量）を初期化
		thresholds = new ThresholdDeterminer[ 4 ];
		thresholds[ 0 ] = new ThresholdByAverage();
		thresholds[ 1 ] = new ThresholdByProbability();
		thresholds[ 2 ] = new ThresholdByCumulative();
		thresholds[ 3 ] = new ThresholdByMinimization();

		// 利用可能な閾値決定モジュール（２次元の特徴量）を初期化
		thresholds2d = new ThresholdDeterminer2D[ 3 ];
		thresholds2d[ 0 ] = new Threshold2DByGaussian1();
		thresholds2d[ 1 ] = new Threshold2DByGaussian2();
		thresholds2d[ 2 ] = new Threshold2DByGaussian3();

		// 文字画像判別モジュールの生成
		recognizer = new CharacterRecognizer2D();
		recognizer.setFeatureEvaluater( features[ 0 ] );
		recognizer.setThresholdDeterminer( thresholds[ 0 ] );
		recognizer.setFeatureEvaluater( 1, features[ 1 ] );
		recognizer.setThresholdDeterminer2D( thresholds2d[ 0 ] );

		// グラフ描画モジュールの生成
		graph_viewr = new GraphViewer();

		// 開始時の実行モードの設定
//		mode = FEATURE_MODE;
		mode = RECOGNITION_MODE;

		// 開始時の表示メッセージの設定
		error_message = "初期化・画像認識処理の実行中 ...";


		// 全サンプル画像の読み込み
		loadSampleImages();


		// UI部品の初期化
		initUIComponents();

		// UI部品の表示を更新
		updateUIComponents();
	}

	// 開始処理
	public void start()
	{
		// 文字画像判別テストの実行
		recognitionTest();
	}


	//
	//  メイン処理
	//

	// 画像振り分け

	// サンプル画像を使った文字画像認識のテスト
	public void  recognitionTest()
	{
 		// 学習・評価に用いるサンプル画像の決定
		sampleDistribution();

		// 全てのサンプル画像を使って学習
		recognizer.train( training_images0, training_images1 );

		// 全てのサンプル画像を使って誤認識率を計算
		int  error_count[] = { 0, 0 };
		int  char_no;
		error_count[ 0 ] = 0;
		error_count[ 1 ] = 0;
		for ( int i=0; i<evaluation_images0.length; i++ )
		{
			char_no = recognizer.recognizeCharacter( evaluation_images0[ i ] );
			if ( char_no != 0 )
				error_count[ 0 ] ++;
		}
		for ( int i=0; i<evaluation_images1.length; i++ )
		{
			char_no = recognizer.recognizeCharacter( evaluation_images1[ i ] );
			if ( char_no != 1 )
				error_count[ 1 ] ++;
		}
		error0 = (float) error_count[ 0 ] / evaluation_images0.length;
		error1 = (float) error_count[ 1 ] / evaluation_images1.length;
		error = (float) ( error_count[ 0 ] + error_count[ 1 ] ) / (float) ( evaluation_images0.length + evaluation_images1.length );

		// 特徴空間・閾値などをグラフに設定
		recognizer.drawGraph( graph_viewr );

		// エラーメッセージを解除（メイン画面の描画開始）
		error_message = "";

		// 画面の再描画
		repaint();
	}


	//
	//  ユーザインターフェース処理
	//

	// UI部品の初期化
	protected void  initUIComponents()
	{
		// ユーザインターフェース用オブジェクトの構築
		Container cp = getContentPane();
		cp.setLayout( new BorderLayout() );
		cp.setBackground( Color.WHITE );

		// インターフェース部品の配置方法の設定
		ui_panel = new JPanel();
		GridBagLayout  ui_grid = new GridBagLayout();
		GridBagConstraints  ui_grid_label = new GridBagConstraints();
		GridBagConstraints  ui_grid_box = new GridBagConstraints();
		ui_grid_label.gridwidth = 1;
		ui_grid_label.anchor = GridBagConstraints.EAST;
		ui_grid_box.anchor = GridBagConstraints.WEST;
		ui_grid_box.gridwidth = GridBagConstraints.REMAINDER;
		ui_panel.setLayout( ui_grid );
		cp.add( ui_panel, BorderLayout.NORTH );

		// モード選択のためのラジオボタンを追加
		mode_button_label = new JLabel( "実行モード： " );
		mode_r_bottun = new JRadioButton( "文字画像認識", true );
		mode_r2_bottun = new JRadioButton( "文字画像認識(2D）", true );
		mode_f_bottun = new JRadioButton( "特徴量表示", false );
		ModeBottunListener  mb_listener = new ModeBottunListener();
		mode_r_bottun.addActionListener( mb_listener );
		mode_r2_bottun.addActionListener( mb_listener );
		mode_f_bottun.addActionListener( mb_listener );
		ui_grid.setConstraints( mode_button_label, ui_grid_label );
		ui_grid.setConstraints( mode_r_bottun, ui_grid_label );
		ui_grid.setConstraints( mode_r2_bottun, ui_grid_label );
		ui_grid.setConstraints( mode_f_bottun, ui_grid_box );
		ui_panel.add( mode_button_label );
		ui_panel.add( mode_r_bottun );
		ui_panel.add( mode_r2_bottun );
		ui_panel.add( mode_f_bottun );
		ButtonGroup  mode_group = new ButtonGroup();
		mode_group.add( mode_r_bottun );
		mode_group.add( mode_r2_bottun );
		mode_group.add( mode_f_bottun );

		// 閾値の計算方法選択のためのコンボボックスの追加
		threshold_label = new JLabel( "閾値の計算方法： " );
		threshold_list = new JComboBox();
		threshold_list.addActionListener( new ThresholdListListener() );
		ui_grid.setConstraints( threshold_label, ui_grid_label );
		ui_grid.setConstraints( threshold_list, ui_grid_box );
		ui_panel.add( threshold_label );
		ui_panel.add( threshold_list );

		// コンボボックスに閾値の計算方法の名前を追加
		for ( int i=0; i<thresholds.length; i++ )
		{
			if ( thresholds[ i ] != null )
				threshold_list.addItem( (String) thresholds[ i ].getThresholdName() );
		}

		// 閾値の計算方法（２次元）選択のためのコンボボックスの追加
		threshold2_label = new JLabel( "閾値の計算方法： " );
		threshold2_list = new JComboBox();
		ui_grid.setConstraints( threshold2_label, ui_grid_label );
		ui_grid.setConstraints( threshold2_list, ui_grid_box );
		ui_panel.add( threshold2_label );
		ui_panel.add( threshold2_list );

		// コンボボックスに閾値の計算方法（２次元）の名前を追加
		for ( int i=0; i<thresholds2d.length; i++ )
		{
			threshold2_list.addItem( (String) thresholds2d[ i ].getThresholdName() );
			if ( recognizer.getThresholdDeterminer2D() == thresholds2d[ i ])
			threshold2_list.setSelectedIndex( i );
		}
		threshold2_list.addActionListener( new Threshold2DListListener() );

		// 特徴量の計算方法選択のためのコンボボックスの追加
		feature_label = new JLabel[ 2 ];
		feature_list = new JComboBox[ 2 ];
		for ( int i=0; i<2; i++ )
		{
			feature_label[ i ] = new JLabel( "特徴量" + (i + 1) + "の計算方法： " );
			ui_grid.setConstraints( feature_label[ i ], ui_grid_label );
			ui_panel.add( feature_label[ i ] );

			feature_list[ i ] = new JComboBox();
			ui_grid.setConstraints( feature_list[ i ], ui_grid_box );
			ui_panel.add( feature_list[ i ] );
		}

		// コンボボックスに特徴量の計算方法の名前を追加
		for ( int i=0; i<2; i++ )
		{
			for ( int j=0; j<features.length; j++ )
			{
				if ( features[ j ] != null )
				{
					feature_list[ i ].addItem( (String) features[ j ].getFeatureName() );
					if ( recognizer.getFeatureEvaluater( i ) == features[ j ])
						feature_list[ i ].setSelectedIndex( j );
				}
			}
			feature_list[ i ].addActionListener( new FeatureListListener() );
		}

		// サンプル画像選択のためのコンボボックスの追加
		image_label = new JLabel( "サンプル画像： " );
		image_list = new JComboBox();
		ui_grid.setConstraints( image_label, ui_grid_label );
		ui_grid.setConstraints( image_list, ui_grid_box );
		ui_panel.add( image_label );
		ui_panel.add( image_list );
		image_list.setEnabled( true );
		image_list.setEditable( false );

		// コンボボックスに全サンプル画像の名前を追加
		if ( image_index != null )
		{
			Set  image_names = image_index.entrySet();
			Iterator  i = image_names.iterator();
			while ( i.hasNext() )
			{
				Map.Entry  entry = (Map.Entry) i.next();
				image_list.addItem( (String) entry.getKey() );
			}
			image_list.addActionListener( new ImageListListener() );
		}

		// メイン画面の描画領域を追加
		screen = new MainScreen();
		cp.add( screen, BorderLayout.CENTER );
		screen.addComponentListener( new MainScreenListener() );
	}

	// UI部品の表示を更新
	protected void  updateUIComponents()
	{
		if ( mode == RECOGNITION_MODE )
		{
			mode_r_bottun.setSelected( true );
			mode_r2_bottun.setSelected( false );
			mode_f_bottun.setSelected( false );
			threshold_label.setVisible( true );
			threshold_list.setVisible( true );
			threshold2_label.setVisible( false );
			threshold2_list.setVisible( false );
			feature_label[ 1 ].setVisible( false );
			feature_list[ 1 ].setVisible( false );
			image_label.setVisible( false );
			image_list.setVisible( false );
		}
		else if ( mode == RECOGNITION_2D_MODE )
		{
			mode_r_bottun.setSelected( false );
			mode_r2_bottun.setSelected( true );
			mode_f_bottun.setSelected( false );
			threshold_label.setVisible( false );
			threshold_list.setVisible( false );
			threshold2_label.setVisible( true );
			threshold2_list.setVisible( true );
			feature_label[ 1 ].setVisible( true );
			feature_list[ 1 ].setVisible( true );
			image_label.setVisible( false );
			image_list.setVisible( false );
		}
		else
		{
			mode_r_bottun.setSelected( false );
			mode_r2_bottun.setSelected( false );
			mode_f_bottun.setSelected( true );
			feature_label[ 1 ].setVisible( false );
			feature_list[ 1 ].setVisible( false );
			threshold_list.setVisible( false );
			threshold_label.setVisible( false );
			threshold2_label.setVisible( false );
			threshold2_list.setVisible( false );
			image_label.setVisible( true );
			image_list.setVisible( true );
		}
	}

	// 実行モード選択処理のためのリスナクラス（内部クラス）
	class  ModeBottunListener implements ActionListener
	{
		// アイテムが選択された時に呼ばれる処理
		public void  actionPerformed( ActionEvent e )
		{
			// 選択されたボタンを取得
			JRadioButton  selected = (JRadioButton) e.getSource();

			// 選択されたボタンに応じて実行モードの変更
			if ( selected == mode_r_bottun )
			{
				mode = RECOGNITION_MODE;
				recognizer.setDimension( 1 );
			}
			else if ( selected == mode_r2_bottun )
			{
				mode = RECOGNITION_2D_MODE;
				recognizer.setDimension( 2 );
			}
			else if ( selected == mode_f_bottun )
				mode = FEATURE_MODE;

			// 実行モードに応じてコンポーネントの表示を変更
			updateUIComponents();

			// 文字画像判別テストを再度実行
			if ( ( mode == RECOGNITION_MODE ) || ( mode == RECOGNITION_2D_MODE ) )
				recognitionTest();

			// 選択画像の特徴量を計算
			if ( mode == FEATURE_MODE )
			{
				// 選択された画像を取得
				BufferedImage  selected_image = null;
				String  selected_image_name = (String) image_list.getSelectedItem();
				if ( ( selected_image_name != null ) && ( image_index != null ) )
					selected_image = (BufferedImage) image_index.get( selected_image_name );

				// 現在の特徴量計算モジュールを使って画像の特徴量を計算
				if ( selected_image != null )
					recognizer.getFeatureEvaluater().evaluate( selected_image );
			}

			// 全体を再描画
			repaint();
		}
	}

	// 閾値の計算方法の選択処理のためのリスナクラス（内部クラス）
	class  ThresholdListListener implements ActionListener
	{
		// アイテムが選択された時に呼ばれる処理
		public void  actionPerformed( ActionEvent e )
		{
			// 選択された閾値の計算方法のインデックスを取得
			int  no = threshold_list.getSelectedIndex();

			// 選択されたインデックスが無効であれば何もせず終了
			if ( ( no == -1 ) || ( thresholds[ no ] == null ) )
				return;

			// 選択された閾値の計算方法が現在のものと同じで有れば何もせず終了
			if ( thresholds[ no ] == recognizer.getThresholdDeterminer() )
				return;

			// 選択された閾値の計算方法を設定
			recognizer.setThresholdDeterminer( thresholds[ no ] );

			// 文字画像判別テストを再度実行
			recognitionTest();

			// 全体を再描画
			repaint();
		}
	}

	// 閾値の計算方法（２次元）の選択処理のためのリスナクラス（内部クラス）
	class  Threshold2DListListener implements ActionListener
	{
		// アイテムが選択された時に呼ばれる処理
		public void  actionPerformed( ActionEvent e )
		{
			// 選択された閾値の計算方法のインデックスを取得
			int  no = threshold2_list.getSelectedIndex();

			// 選択されたインデックスが無効であれば何もせず終了
			if ( ( no == -1 ) || ( thresholds[ no ] == null ) )
				return;

			// 選択された閾値の計算方法が現在のものと同じで有れば何もせず終了
			if ( thresholds2d[ no ] == recognizer.getThresholdDeterminer2D() )
				return;

			// 選択された閾値の計算方法を設定
			recognizer.setThresholdDeterminer2D( thresholds2d[ no ] );

			// 文字画像判別テストを再度実行
			recognitionTest();

			// 全体を再描画
			repaint();
		}
	}

	// 特徴量の計算方法の選択処理のためのリスナクラス（内部クラス）
	class  FeatureListListener implements ActionListener
	{
		// アイテムが選択された時に呼ばれる処理
		public void  actionPerformed( ActionEvent e )
		{
			// アイテムが変更されたリストを取得
			JComboBox  changed_list = (JComboBox) e.getSource();
			int  dim;
			if ( changed_list == feature_list[ 0 ] )
				dim = 0;
			else
				dim = 1;

			// 選択された特徴量の計算方法のインデックスを取得
			int  no = feature_list[ dim ].getSelectedIndex();

			// 選択されたインデックスが無効なら何もせず終了
			if ( ( no == -1 ) || ( features[ no ] == null ) )
				return;
			// 選択された閾値の計算方法が現在のものと同じなら何もせず終了
			if ( features[ no ] == recognizer.getFeatureEvaluater( dim ) )
				return;

			// 選択された閾値の計算方法を設定
			recognizer.setFeatureEvaluater( dim, features[ no ] );

			// 文字画像判別テストを再度実行
			recognitionTest();

			// 全体を再描画
			repaint();
		}
	}

	// 表示画像の選択処理のためのリスナクラス（内部クラス）
	class  ImageListListener implements ActionListener
	{
		// アイテムが選択された時に呼ばれる処理
		public void  actionPerformed( ActionEvent e )
		{
			// 選択された画像を取得
			String  selected_image_name = (String) image_list.getSelectedItem();
			BufferedImage  selected_image = (BufferedImage) image_index.get( selected_image_name );

			// 現在の特徴量計算モジュールを使って画像の特徴量を計算
			recognizer.getFeatureEvaluater().evaluate( selected_image );

			// 全体を再描画
			repaint();
		}
	}

	// メイン画面のサイズ変更を検知するためのリスナクラス（内部クラス）
	class  MainScreenListener implements ComponentListener
	{
		// 画面上部の余白（認識率表示のためのスペース）
		final public int  top_margin = 72;

		// サイズ変更された時に呼ばれる処理
		public void componentResized( ComponentEvent e )
		{
			// グラフの描画範囲を設定
			graph_viewr.setDrawArea( 0, top_margin, screen.getWidth(), screen.getHeight() );

			// 全体を再描画
			repaint();
		}
		public void componentMoved( ComponentEvent e )
		{
		}
		public void componentShown( ComponentEvent e )
		{
		}
		public void componentHidden( ComponentEvent e )
		{
		}
	}

	// メイン画面描画のためのコンポーネントクラス（内部クラス）
	class  MainScreen extends JComponent
	{
		// 描画処理
		public void  paint( Graphics g )
		{
			// 親コンポーネントの描画
			super.paint( g );

			// エラーメッセージが設定されていれば表示
			if ( ( error_message != null ) && ( error_message.length() > 0 ) )
			{
				g.drawString( error_message, 16, 16 );
				return;
			}

			// 文字画像認識モードでは特徴空間・認識率を描画
			if ( ( mode == RECOGNITION_MODE ) || ( mode == RECOGNITION_2D_MODE ) )
			{
				// 特徴空間を表すグラフを描画
				graph_viewr.paint( g );

				// 誤認識率を表示
				String  message;
				g.setColor( Color.BLACK );
				message = "誤認識率: " + error;
				g.drawString( message, 16, 16 );
				message = character0 + "の誤認識率: " + error0;
				g.drawString( message, 16, 32 );
				message = character1 + "の誤認識率: " + error1;
				g.drawString( message, 16, 48 );
				if ( mode == RECOGNITION_MODE )
				{
					message = "閾値: " + recognizer.getThresholdDeterminer().getThreshold();
					g.drawString( message, 16, 64 );
				}
			}
			// 特徴量表示モードでは選択画像の特徴量計算結果を描画
			else if ( mode == FEATURE_MODE )
			{
				// 選択画像の特徴量の計算結果を描画
				recognizer.getFeatureEvaluater().paintImageFeature( g );
			}
		}
	}


	//
	//  サンプル画像の読み込みのための内部メソッド
	//

	// サンプル画像の読み込み
	public void  loadSampleImages()
	{
		// サンプル画像の読み込み
		if ( ( image_name0.length() > 0 ) && ( image_name1.length() > 0 ) )
		{
			sample_images0 = loadBufferedImages( image_dir + image_name0, 1, num_images0, image_digits, image_ext );
			sample_images1 = loadBufferedImages( image_dir + image_name1, 1, num_images1, image_digits, image_ext );
		}
		else
		{
			error_message = "読み込む画像名が指定されていません。";
		}

		// 画像ファイルの読み込みに成功したかどうかを確認
		for ( int i=0; i<sample_images0.length; i++ )
		{
			if ( sample_images0[ i ] == null )
			{
				error_message = image_name0 + "*" + image_ext + " の " + (i + 1) + "番目の画像の読み込みに失敗しました。";
				sample_images0 = null;
				sample_images1 = null;
				return;
			}
		}
		for ( int i=0; i<sample_images1.length; i++ )
		{
			if ( sample_images1[ i ] == null )
			{
				error_message = image_name1 + "*" + image_ext + " の " + (i + 1) + "番目の画像の読み込みに失敗しました。";
				sample_images0 = null;
				sample_images1 = null;
				return;
			}
		}

		// 全ての画像をインデックスに記録する
		image_index = new TreeMap();
		String  name;
		for ( int i=0; i<sample_images0.length; i++ )
		{
			name = "" + (i + 1);
			while ( name.length() < image_digits )
				name = "0" + name;
			name = image_name0 + name;
			image_index.put( name, sample_images0[ i ] );
		}
		for ( int i=0; i<sample_images1.length; i++ )
		{
			name = "" + (i + 1);
			while ( name.length() < image_digits )
				name = "0" + name;
			name = image_name1 + name;
			image_index.put( name, sample_images1[ i ] );
		}
	}

	// 連番画像の配列への読み込み
	protected BufferedImage[]  loadBufferedImages( String prefix, int count_start, int count_end, int count_width, String suffix )
	{
		// 読み込んだ画像を格納する配列のサイズを初期化
		BufferedImage[]  images = new BufferedImage[ count_end - count_start + 1 ];

		// 連番画像を順に読み込み
		for ( int i=count_start; i<=count_end; i++ )
		{
			// ファイル名を作成
			String  filename = "" + i;
			while ( filename.length() < count_width )
				filename = "0" + filename;
			filename = prefix + filename + suffix;

			// 画像を読み込み
			images[ i - count_start ] = getBufferedImage( filename );
		}

		return  images;
	}

	// Image I/O を使った画像の読み込み
	protected BufferedImage getBufferedImage( String filename )
	{
		try
		{
			java.io.File  file = new java.io.File( filename );
			BufferedImage image = javax.imageio.ImageIO.read( file );
			return  image;
		}
		catch ( Exception e )
		{
			return  null;
		}
	}

	// 学習・評価に用いるサンプル画像の決定
	public void  sampleDistribution()
	{
		// @param fold_num: CrossValidation法の際、何番目のグループかを表す
		// 					cv_evaluation_foldに代入する

		// サンプル画像が読み込まれていなければ終了
		if ( ( sample_images0 == null ) || ( sample_images1 == null ) )
			return;

		// 学習用画像の配列を削除する
		training_images0 = null;
		training_images1 = null;

		// 評価用画像の配列を削除する
		evaluation_images0 = null;
		evaluation_images1 = null;

		// 全てのサンプル画像を学習と評価に使用
		if ( distribution_method == DistributionMethod.USE_ALL_SAMPLES )
		{
			// 全てのサンプル画像を学習用画像の配列にコピー
			training_images0 = new BufferedImage[ sample_images0.length ];
			for ( int i=0; i<sample_images0.length; i++ )
				training_images0[ i ] = sample_images0[ i ];
			training_images1 = new BufferedImage[ sample_images1.length ];
			for ( int i=0; i<sample_images1.length; i++ )
				training_images1[ i ] = sample_images1[ i ];

			// 全てのサンプル画像を評価用画像の配列にコピー
			evaluation_images0 = new BufferedImage[ sample_images0.length ];
			for ( int i=0; i<sample_images0.length; i++ )
				evaluation_images0[ i ] = sample_images0[ i ];
			evaluation_images1 = new BufferedImage[ sample_images1.length ];
			for ( int i=0; i<sample_images1.length; i++ )
				evaluation_images1[ i ] = sample_images1[ i ];
		}

		// Cross Validation 法を使用
		if ( distribution_method == DistributionMethod.CROSS_VALIDATION )
		{
			// 何番目の集まりを評価用に使用するかを表す
			cv_evaluation_fold = 3;

		// 全サンプル画像の何番目～何番目のデータを評価に使用するかを決定 （文字0）
			//（cv_number_of_folds, cv_evaluation_fold をもとに決定）
			int evaluation_begin0;  // 評価データの先頭
			int evaluation_end0;    // 評価データの最後尾
			int evaluation_count0;  // 評価データの個数

			// sample_images0.length×cv_evaluation_fold / cv_number_of_folds

			evaluation_begin0 = (sample_images0.length / cv_number_of_folds) * cv_evaluation_fold;
			evaluation_end0 = ((sample_images0.length / cv_number_of_folds)) * (cv_evaluation_fold + 1) - 1;
			evaluation_count0 = evaluation_end0 - evaluation_begin0 + 1;

			//  ここは各自で完成
			// sample_images0 全画像が入っている.
//			// cv_number_of_folds is Number of groups to divide.
//			// cv_evaluation_fold is Group number used for evaluation.
//			if (sample_images0.length % cv_number_of_folds == 0) {
//				evaluation_count0 = sample_images0.length / cv_number_of_folds;
//				evaluation_begin0 = (cv_evaluation_fold - 1) * evaluation_count0;
//				evaluation_end0 = evaluation_begin0 + evaluation_count0;
//			} else {
//				// java切り捨て 88で考える fold is 10
//				int calcMinPicPerFolds = sample_images0.length / cv_number_of_folds; // 8 <= 8.8
//				int minTotalPic = cv_number_of_folds * calcMinPicPerFolds; // 80
//				int surplus = sample_images0.length - minTotalPic;
//
//				evaluation_count0 = calcMinPicPerFolds;
//				if(cv_evaluation_fold < surplus + 1) {
//					evaluation_count0 = calcMinPicPerFolds + 1;
//					evaluation_begin0 = (cv_evaluation_fold - 1) * evaluation_count0;
//					evaluation_end0 = evaluation_begin0 + evaluation_count0;
//				} else {
//
//				}
//
//			}


		// 全サンプル画像の何番目～何番目のデータを評価に使用するかを決定 （文字1）
			//（cv_number_of_folds, cv_evaluation_fold をもとに決定）
			int evaluation_begin1;  // 評価データの先頭
			int evaluation_end1;    // 評価データの最後尾
			int evaluation_count1;  // 評価データの個数

			//  ここは各自で完成
			evaluation_begin1 = (sample_images1.length / cv_number_of_folds) * cv_evaluation_fold;
			evaluation_end1 = ((sample_images1.length / cv_number_of_folds)) * (cv_evaluation_fold + 1) - 1;
			evaluation_count1 = evaluation_end1 - evaluation_begin1 + 1;


		// 全サンプル画像を評価用画像と学習用画像の配列に分配 （文字0）

			//  ここは各自で完成
			int arrayTotalSize = sample_images0.length - evaluation_count0;
			training_images0 = new BufferedImage[arrayTotalSize];
			evaluation_images0 = new BufferedImage[evaluation_count0];

			int cntE = 0;
			for(int i = 0; i < sample_images0.length; i++) {
				if(evaluation_begin0 <= i && i <= evaluation_end0) {
					evaluation_images0[cntE] = sample_images0[i];
					cntE = cntE + 1;
				}
				if (i < evaluation_begin0) {
					training_images0[i] = sample_images0[i];
				} else if (i > evaluation_end0) {
					training_images0[i - evaluation_count0] = sample_images0[i];
				}
			}

		// 全サンプル画像を評価用画像と学習用画像の配列に分配 （文字1）

			//  ここは各自で完成
			arrayTotalSize = sample_images1.length - evaluation_count1;
			training_images1 = new BufferedImage[arrayTotalSize];
			evaluation_images1 = new BufferedImage[evaluation_count1];

			cntE = 0;
			for(int i = 0; i < sample_images1.length; i++) {
				if(evaluation_begin1 <= i && i <= evaluation_end1) {
					evaluation_images1[cntE] = sample_images1[i];
					cntE = cntE + 1;
				}
				if (i < evaluation_begin1) {
					training_images1[i] = sample_images1[i];
				} else if (i > evaluation_end1) {
					training_images1[i - evaluation_count1] = sample_images1[i];
				}
			}

/*
			System.out.println(training_images0 + " Train image0");
			System.out.println(training_images1 + " Train image1");
			System.out.println(evaluation_images0 + " Train image0");
			System.out.println(evaluation_images1 + " Train image0");
*/
		}
	}


	//
	//  メイン関数
	//
	public static void  main( String[] args )
	{
		// アプレットを起動
		Frame  frame = new Frame( "Recognition App" );
		frame.addWindowListener( new WindowAdapter()
			{
				public void windowClosing( WindowEvent evt )
				{
					System.exit(0);
				}
			} );
		RecognitionApp applet = new RecognitionApp();
		applet.init();
		frame.add( applet );
		frame.setSize( 480, 480 );
		frame.show();
		applet.start();
	}
}