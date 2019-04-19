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
	protected CharacterRecognizer  recognizer;

	// 利用可能な特徴量計算モジュール
	protected FeatureEvaluater   features[];

	// 利用可能な閾値決定モジュール（１次元の特徴量）
	protected ThresholdDeterminer  thresholds[];

	// グラフ描画モジュール
	protected GraphViewer  graph_viewr;

	// 実行モード
	protected int  mode;
	protected final int  RECOGNITION_MODE = 1;
	protected final int  FEATURE_MODE = 2;

	// 学習・文字認識テストの結果（誤認識率）
	protected float  error, error0, error1;

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
	protected MainScreen  screen;


	// 初期化処理
	public void  init()
	{
		// 利用可能な特徴量計算モジュールを初期化
		features = new FeatureEvaluater[ 1 ];
		features[ 0 ] = new FeatureLeftLinerity();

		// 利用可能な閾値決定モジュール（１次元の特徴量）を初期化
		thresholds = new ThresholdDeterminer[ 1 ];
		thresholds[ 0 ] = new ThresholdByAverage();

		// 文字画像判別モジュールの生成
		recognizer = new CharacterRecognizer();
		recognizer.setFeatureEvaluater( features[ 0 ] );
		recognizer.setThresholdDeterminer( thresholds[ 0 ] );

		// グラフ描画モジュールの生成
		graph_viewr = new GraphViewer();

		// 開始時の実行モードの設定
		mode = FEATURE_MODE;


		// 全サンプル画像の読み込み
		loadSampleImages();


		// UI部品の初期化
		initUIComponents();

		// UI部品の表示を更新
		updateUIComponents();
	}

	// 開始処理
	public void  start()
	{
		// 文字画像判別テストの実行
		recognitionTest();
	}


	//
	//  メイン処理
	//

	// サンプル画像を使った文字画像認識のテスト
	public void  recognitionTest()
	{
		// 要実装
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
		mode_f_bottun = new JRadioButton( "特徴量表示", false );
		ModeBottunListener  mb_listener = new ModeBottunListener();
		mode_r_bottun.addActionListener( mb_listener );
		mode_f_bottun.addActionListener( mb_listener );
		ui_grid.setConstraints( mode_button_label, ui_grid_label );
		ui_grid.setConstraints( mode_r_bottun, ui_grid_label );
		ui_grid.setConstraints( mode_f_bottun, ui_grid_box );
		ui_panel.add( mode_button_label );
		ui_panel.add( mode_r_bottun );
		ui_panel.add( mode_f_bottun );
		ButtonGroup  mode_group = new ButtonGroup();
		mode_group.add( mode_r_bottun );
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

		// 特徴量の計算方法選択のためのコンボボックスの追加
		feature_label = new JLabel[ 1 ];
		feature_list = new JComboBox[ 1 ];
		for ( int i=0; i<1; i++ )
		{
			feature_label[ i ] = new JLabel( "特徴量" + (i + 1) + "の計算方法： " );
			ui_grid.setConstraints( feature_label[ i ], ui_grid_label );
			ui_panel.add( feature_label[ i ] );

			feature_list[ i ] = new JComboBox();
			ui_grid.setConstraints( feature_list[ i ], ui_grid_box );
			ui_panel.add( feature_list[ i ] );
		}

		// コンボボックスに特徴量の計算方法の名前を追加
		for ( int i=0; i<1; i++ )
		{
			for ( int j=0; j<features.length; j++ )
			{
				if ( features[ j ] != null )
				{
					feature_list[ i ].addItem( (String) features[ j ].getFeatureName() );
					if ( recognizer.getFeatureEvaluater() == features[ j ])
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
			mode_f_bottun.setSelected( false );
			threshold_label.setVisible( true );
			threshold_list.setVisible( true );
			image_label.setVisible( false );
			image_list.setVisible( false );
		}
		else
		{
			mode_r_bottun.setSelected( false );
			mode_f_bottun.setSelected( true );
			threshold_list.setVisible( false );
			threshold_label.setVisible( false );
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
				mode = RECOGNITION_MODE;
			else if ( selected == mode_f_bottun )
				mode = FEATURE_MODE;

			// 実行モードに応じてコンポーネントの表示を変更
			updateUIComponents();

			// 文字画像判別テストを再度実行
			if ( mode == RECOGNITION_MODE )
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

	// 特徴量の計算方法の選択処理のためのリスナクラス（内部クラス）
	class  FeatureListListener implements ActionListener
	{
		// アイテムが選択された時に呼ばれる処理
		public void  actionPerformed( ActionEvent e )
		{
			// 選択された特徴量の計算方法のインデックスを取得
			int  no = feature_list[ 0 ].getSelectedIndex();

			// 選択されたインデックスが無効なら何もせず終了
			if ( ( no == -1 ) || ( features[ no ] == null ) )
				return;
			// 選択された閾値の計算方法が現在のものと同じなら何もせず終了
			if ( features[ no ] == recognizer.getFeatureEvaluater() )
				return;

			// 選択された閾値の計算方法を設定
			recognizer.setFeatureEvaluater( features[ no ] );

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

			// 文字画像認識モードでは特徴空間・認識率を描画
			if ( mode == RECOGNITION_MODE )
			{
				// 特徴空間を表すグラフを描画
				graph_viewr.paint( g );

				// 誤認識率を表示
				String  message;
				g.setColor( Color.BLACK );
				message = "誤認識率: " + error;
				g.drawString( message, 16, 16 );
				message = "8 の誤認識率: " + error0;
				g.drawString( message, 16, 32 );
				message = "B の誤認識率: " + error1;
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
//	public void  loadSampleImages()
//	{
//		// 要実装 -> 済
//		// 8のサンプル画像の読み込み
//
//		sample_images0 = loadBufferdImages("Samples8Bgif/pic8_", 1, 55, 3, "gif");
//	}

	// サンプル画像の読み込み
	public void  loadSampleImages()
	{
		// 8 のサンプル画像を読み込み（Samples8Bgif/pic8_001.gif ～ pic8_105.gif の105枚）
		sample_images0 = loadBufferedImages( "Samples8Bgif/pic8_", 1, 105, 3, ".gif" );

		// B のサンプル画像を読み込み（Samples8Bgif/picB_001.gif ～ picB_105.gif の105枚）
		sample_images1 = loadBufferedImages( "Samples8Bgif/picB_", 1, 105, 3, ".gif" );

		// 全ての画像をインデックスに記録する
		image_index = new TreeMap();
		String  name;
		for ( int i=0; i<sample_images0.length; i++ )
		{
			name = "" + (i + 1);
			while ( name.length() < 3 )
				name = "0" + name;
			name = "pict8_" + name;
			image_index.put( name, sample_images0[ i ] );
		}
		for ( int i=0; i<sample_images1.length; i++ )
		{
			name = "" + (i + 1);
			while ( name.length() < 3 )
				name = "0" + name;
			name = "pictB_" + name;
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
		RecognitionApp  applet = new RecognitionApp();
		applet.init();
		frame.add( applet );
		frame.setSize( 480, 480 );
		frame.show();
		applet.start();
	}
}