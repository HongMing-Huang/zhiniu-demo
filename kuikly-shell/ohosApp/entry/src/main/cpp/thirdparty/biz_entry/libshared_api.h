#ifndef KONAN_LIBSHARED_H
#define KONAN_LIBSHARED_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libshared_KBoolean;
#else
typedef _Bool           libshared_KBoolean;
#endif
typedef unsigned short     libshared_KChar;
typedef signed char        libshared_KByte;
typedef short              libshared_KShort;
typedef int                libshared_KInt;
typedef long long          libshared_KLong;
typedef unsigned char      libshared_KUByte;
typedef unsigned short     libshared_KUShort;
typedef unsigned int       libshared_KUInt;
typedef unsigned long long libshared_KULong;
typedef float              libshared_KFloat;
typedef double             libshared_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libshared_KVector128;
typedef void*              libshared_KNativePtr;
struct libshared_KType;
typedef struct libshared_KType libshared_KType;

typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Byte;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Short;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Int;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Long;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Float;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Double;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Char;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Boolean;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Unit;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_UByte;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_UShort;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_UInt;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_ULong;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_local_Watchlist;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_mock_MarketStore;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiService;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_mock_MockMarketRepository;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_collections_List;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_mock_MockAiService;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_MarketRepository;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AiInsight;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_MarketBreadth;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_Timeframe;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_StockQuote;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_MarketNewsItem;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Any;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_AgentStage;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_AgentChatResult;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_SectorRow;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_SectorResult;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_ResearchStageEvent;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_PopularStock;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_PopularityResult;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_CandleSeriesResult;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_AgentResearchResult;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_GatewayStatus;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_data_remote_GatewayMarketClient;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_NO_NETWORK;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_RATE_LIMIT;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_AUTH_INVALID;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_UPSTREAM_5XX;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_JSON_SCHEMA_FAILED;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_NIL;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_Unknown;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_AppError_Companion;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Throwable;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_MessageRole;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_MessageRole_USER;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_MessageRole_ASSISTANT;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_MessageRole_SYSTEM;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_MessageRole_TOOL;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_ChatMessage;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_CandleDirection;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_CandleDirection_UP;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_CandleDirection_DOWN;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_CandleDirection_FLAT;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_CandleGeometry;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_KLineChart;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Pair;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_SseParser;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_SseParser_EventKind;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_SseParser_Frame;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_SseParser_EventKind_DATA;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_SseParser_EventKind_STEP_PROGRESS;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_SseParser_EventKind_ERROR;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_SseParser_EventKind_DONE;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_OrderBookLevel;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_StockFundamentals;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_MarketIndex;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_Candle;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_TechnicalIndicator;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_Timeframe_INTRADAY;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_Timeframe_DAY;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_Timeframe_WEEK;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_Timeframe_MONTH;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_ChatSession;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_TodoItem;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_model_TodoStore;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiBlock;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiBlock_Text;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_domain_repository_MetricCell;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_AiChatMessage;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_tencent_kuikly_core_base_ViewContainer;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_tencent_kuikly_core_views_CanvasContext;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_tencent_kuikly_core_base_Color;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_StockFacts;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_tencent_kuikly_core_base_Animation;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_tencent_kuikly_core_base_Attr;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Function1;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Function0;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdSpan;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Divider;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_AppColors;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Triple;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator_NONE;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator_MA;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator_MACD;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator_RSI;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_collections_Set;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_market_StockColumns;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_market_RankRow;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_SEARCH;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_BACK;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_STAR;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_STAR_FILLED;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_FILTER;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_THEME;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CLOSE;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_SEND;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_AI;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_MORE;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CHART;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CHEVRON_DOWN;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CHEVRON_UP;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CHEVRON_RIGHT;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CALENDAR;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CHECK;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_FILTER_SORT;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_ERROR;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_CHAT;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_IconKind_DATA;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ThemeMode;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ThemeMode_SYSTEM;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ThemeMode_LIGHT;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_ThemeMode_DARK;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_AppTypography;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_AppSpacing;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_AppRadius;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_LightColors;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_DarkColors;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_pages_components_AppTheme;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_com_zhiniu_platform_GatewayTransport;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libshared_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libshared_KBoolean (*IsInstance)(libshared_KNativePtr ref, const libshared_KType* type);
  libshared_kref_kotlin_Byte (*createNullableByte)(libshared_KByte);
  libshared_KByte (*getNonNullValueOfByte)(libshared_kref_kotlin_Byte);
  libshared_kref_kotlin_Short (*createNullableShort)(libshared_KShort);
  libshared_KShort (*getNonNullValueOfShort)(libshared_kref_kotlin_Short);
  libshared_kref_kotlin_Int (*createNullableInt)(libshared_KInt);
  libshared_KInt (*getNonNullValueOfInt)(libshared_kref_kotlin_Int);
  libshared_kref_kotlin_Long (*createNullableLong)(libshared_KLong);
  libshared_KLong (*getNonNullValueOfLong)(libshared_kref_kotlin_Long);
  libshared_kref_kotlin_Float (*createNullableFloat)(libshared_KFloat);
  libshared_KFloat (*getNonNullValueOfFloat)(libshared_kref_kotlin_Float);
  libshared_kref_kotlin_Double (*createNullableDouble)(libshared_KDouble);
  libshared_KDouble (*getNonNullValueOfDouble)(libshared_kref_kotlin_Double);
  libshared_kref_kotlin_Char (*createNullableChar)(libshared_KChar);
  libshared_KChar (*getNonNullValueOfChar)(libshared_kref_kotlin_Char);
  libshared_kref_kotlin_Boolean (*createNullableBoolean)(libshared_KBoolean);
  libshared_KBoolean (*getNonNullValueOfBoolean)(libshared_kref_kotlin_Boolean);
  libshared_kref_kotlin_Unit (*createNullableUnit)(void);
  libshared_kref_kotlin_UByte (*createNullableUByte)(libshared_KUByte);
  libshared_KUByte (*getNonNullValueOfUByte)(libshared_kref_kotlin_UByte);
  libshared_kref_kotlin_UShort (*createNullableUShort)(libshared_KUShort);
  libshared_KUShort (*getNonNullValueOfUShort)(libshared_kref_kotlin_UShort);
  libshared_kref_kotlin_UInt (*createNullableUInt)(libshared_KUInt);
  libshared_KUInt (*getNonNullValueOfUInt)(libshared_kref_kotlin_UInt);
  libshared_kref_kotlin_ULong (*createNullableULong)(libshared_KULong);
  libshared_KULong (*getNonNullValueOfULong)(libshared_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            struct {
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_local_Watchlist (*_instance)();
                libshared_KBoolean (*contains)(libshared_kref_com_zhiniu_data_local_Watchlist thiz, const char* symbol);
                void (*toggle)(libshared_kref_com_zhiniu_data_local_Watchlist thiz, const char* symbol);
              } Watchlist;
            } local;
            struct {
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_mock_MarketStore (*_instance)();
                libshared_kref_com_zhiniu_domain_repository_AiService (*get_aiService)(libshared_kref_com_zhiniu_data_mock_MarketStore thiz);
                libshared_kref_com_zhiniu_data_mock_MockMarketRepository (*get_repository)(libshared_kref_com_zhiniu_data_mock_MarketStore thiz);
                void (*applyLiveQuotes)(libshared_kref_com_zhiniu_data_mock_MarketStore thiz, libshared_kref_kotlin_collections_List quotes);
              } MarketStore;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_mock_MockAiService (*MockAiService)(libshared_kref_com_zhiniu_domain_repository_MarketRepository repository);
                libshared_kref_kotlin_collections_List (*chatReply)(libshared_kref_com_zhiniu_data_mock_MockAiService thiz, const char* sessionId, const char* question);
                libshared_kref_com_zhiniu_domain_model_AiInsight (*insightFor)(libshared_kref_com_zhiniu_data_mock_MockAiService thiz, const char* symbol, libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals fundamentals);
              } MockAiService;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_mock_MockMarketRepository (*MockMarketRepository)();
                void (*applyLiveQuotes)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz, libshared_kref_kotlin_collections_List quotes);
                libshared_kref_com_zhiniu_domain_model_MarketBreadth (*breadth)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz);
                libshared_kref_kotlin_collections_List (*candles)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz, const char* symbol, libshared_kref_com_zhiniu_domain_model_Timeframe timeframe);
                libshared_kref_kotlin_collections_List (*indices)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz);
                libshared_kref_com_zhiniu_domain_model_StockQuote (*quoteOf)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz, const char* symbol);
                libshared_kref_kotlin_collections_List (*search)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz, const char* query);
                libshared_kref_kotlin_collections_List (*spark)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz, const char* symbol);
                libshared_kref_kotlin_collections_List (*stockQuotes)(libshared_kref_com_zhiniu_data_mock_MockMarketRepository thiz);
              } MockMarketRepository;
            } mock;
            struct {
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_MarketNewsItem (*MarketNewsItem)(const char* id, const char* title, const char* summary, const char* publishedAt, const char* source, const char* provider, libshared_KBoolean isStale);
                const char* (*get_id)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                libshared_KBoolean (*get_isStale)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*get_provider)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*get_publishedAt)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*get_summary)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*get_title)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*component4)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*component5)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*component6)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                libshared_KBoolean (*component7)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                libshared_kref_com_zhiniu_data_remote_MarketNewsItem (*copy)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz, const char* id, const char* title, const char* summary, const char* publishedAt, const char* source, const char* provider, libshared_KBoolean isStale);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_MarketNewsItem thiz);
              } MarketNewsItem;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_AgentStage (*AgentStage)(const char* label, const char* source, const char* status);
                const char* (*get_label)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
                const char* (*get_status)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
                libshared_kref_com_zhiniu_data_remote_AgentStage (*copy)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz, const char* label, const char* source, const char* status);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_AgentStage thiz);
              } AgentStage;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_AgentChatResult (*AgentChatResult)(const char* content, libshared_KBoolean isLlm, const char* provider);
                const char* (*get_content)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
                libshared_KBoolean (*get_isLlm)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
                const char* (*get_provider)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
                libshared_KBoolean (*component2)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
                libshared_kref_com_zhiniu_data_remote_AgentChatResult (*copy)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz, const char* content, libshared_KBoolean isLlm, const char* provider);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_AgentChatResult thiz);
              } AgentChatResult;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_SectorRow (*SectorRow)(const char* code, const char* name, libshared_KDouble changePercent, libshared_KInt upCount, libshared_KInt downCount, const char* leadStock, const char* leadSymbol, libshared_KDouble leadChangePercent, libshared_KInt rank);
                libshared_KDouble (*get_changePercent)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*get_code)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KInt (*get_downCount)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KBoolean (*get_isUp)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KDouble (*get_leadChangePercent)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*get_leadStock)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*get_leadSymbol)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*get_name)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KInt (*get_rank)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KInt (*get_upCount)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KDouble (*component3)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KInt (*component4)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KInt (*component5)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*component6)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*component7)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KDouble (*component8)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_KInt (*component9)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                libshared_kref_com_zhiniu_data_remote_SectorRow (*copy)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz, const char* code, const char* name, libshared_KDouble changePercent, libshared_KInt upCount, libshared_KInt downCount, const char* leadStock, const char* leadSymbol, libshared_KDouble leadChangePercent, libshared_KInt rank);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_SectorRow thiz);
              } SectorRow;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_SectorResult (*SectorResult)(libshared_kref_kotlin_collections_List sectors, const char* source, libshared_KBoolean isStale);
                libshared_KBoolean (*get_isStale)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
                libshared_kref_kotlin_collections_List (*get_sectors)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
                libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
                libshared_KBoolean (*component3)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
                libshared_kref_com_zhiniu_data_remote_SectorResult (*copy)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz, libshared_kref_kotlin_collections_List sectors, const char* source, libshared_KBoolean isStale);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_SectorResult thiz);
              } SectorResult;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_ResearchStageEvent (*ResearchStageEvent)(const char* type, const char* stage, const char* label, const char* source);
                const char* (*get_label)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*get_stage)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*get_type)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*component4)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                libshared_kref_com_zhiniu_data_remote_ResearchStageEvent (*copy)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz, const char* type, const char* stage, const char* label, const char* source);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_ResearchStageEvent thiz);
              } ResearchStageEvent;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_PopularStock (*PopularStock)(libshared_KInt rank, const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent);
                libshared_KDouble (*get_changePercent)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                const char* (*get_name)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                libshared_KDouble (*get_price)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                libshared_KInt (*get_rank)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                const char* (*get_symbol)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                libshared_KInt (*component1)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                libshared_KDouble (*component4)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                libshared_KDouble (*component5)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                libshared_kref_com_zhiniu_data_remote_PopularStock (*copy)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz, libshared_KInt rank, const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_PopularStock thiz);
              } PopularStock;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_PopularityResult (*PopularityResult)(libshared_kref_kotlin_collections_List stocks, const char* source, libshared_KBoolean isStale);
                libshared_KBoolean (*get_isStale)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
                libshared_kref_kotlin_collections_List (*get_stocks)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
                libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
                libshared_KBoolean (*component3)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
                libshared_kref_com_zhiniu_data_remote_PopularityResult (*copy)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz, libshared_kref_kotlin_collections_List stocks, const char* source, libshared_KBoolean isStale);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_PopularityResult thiz);
              } PopularityResult;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_CandleSeriesResult (*CandleSeriesResult)(libshared_kref_kotlin_collections_List bars, const char* source, const char* provider, libshared_KBoolean isStale);
                libshared_kref_kotlin_collections_List (*get_bars)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                libshared_KBoolean (*get_isStale)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                const char* (*get_provider)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                libshared_KBoolean (*component4)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                libshared_kref_com_zhiniu_data_remote_CandleSeriesResult (*copy)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz, libshared_kref_kotlin_collections_List bars, const char* source, const char* provider, libshared_KBoolean isStale);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_CandleSeriesResult thiz);
              } CandleSeriesResult;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_AgentResearchResult (*AgentResearchResult)(const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent, const char* direction, libshared_KDouble rangeChangePercent, libshared_KDouble rsi14, libshared_KDouble ma20, libshared_KInt newsCount, const char* newsSource, libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double marketCap, const char* reportDate, const char* financialSource, libshared_kref_kotlin_collections_List riskFlags, libshared_kref_kotlin_collections_List stages, const char* summary, const char* stance, libshared_KDouble confidence, const char* synthesisMode, const char* synthesisProvider, const char* rating, const char* trend, libshared_kref_kotlin_Double pressure, libshared_kref_kotlin_Double support, libshared_kref_kotlin_collections_List bullPoints, libshared_kref_kotlin_collections_List bearPoints, libshared_kref_kotlin_collections_List riskNotes, const char* traderPlan, libshared_kref_kotlin_Double traderEntry, libshared_kref_kotlin_Double traderStop);
                libshared_kref_kotlin_collections_List (*get_bearPoints)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*get_bullPoints)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*get_changePercent)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*get_confidence)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_direction)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_financialSource)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*get_ma20)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*get_marketCap)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_name)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KInt (*get_newsCount)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_newsSource)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*get_pb)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*get_pe)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*get_pressure)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*get_price)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*get_rangeChangePercent)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_rating)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_reportDate)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*get_riskFlags)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*get_riskNotes)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*get_rsi14)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*get_stages)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_stance)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_summary)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*get_support)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_symbol)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_synthesisMode)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_synthesisProvider)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*get_traderEntry)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_traderPlan)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*get_traderStop)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*get_trend)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component10)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*component11)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*component12)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*component13)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component14)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component15)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*component16)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*component17)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component18)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component19)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*component20)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component21)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component22)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component23)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component24)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*component25)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*component26)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*component27)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*component28)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_collections_List (*component29)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*component3)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component30)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*component31)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_kotlin_Double (*component32)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*component4)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*component5)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*component6)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*component7)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KDouble (*component8)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_KInt (*component9)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                libshared_kref_com_zhiniu_data_remote_AgentResearchResult (*copy)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz, const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent, const char* direction, libshared_KDouble rangeChangePercent, libshared_KDouble rsi14, libshared_KDouble ma20, libshared_KInt newsCount, const char* newsSource, libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double marketCap, const char* reportDate, const char* financialSource, libshared_kref_kotlin_collections_List riskFlags, libshared_kref_kotlin_collections_List stages, const char* summary, const char* stance, libshared_KDouble confidence, const char* synthesisMode, const char* synthesisProvider, const char* rating, const char* trend, libshared_kref_kotlin_Double pressure, libshared_kref_kotlin_Double support, libshared_kref_kotlin_collections_List bullPoints, libshared_kref_kotlin_collections_List bearPoints, libshared_kref_kotlin_collections_List riskNotes, const char* traderPlan, libshared_kref_kotlin_Double traderEntry, libshared_kref_kotlin_Double traderStop);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_AgentResearchResult thiz);
              } AgentResearchResult;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_GatewayStatus (*GatewayStatus)(libshared_KBoolean online, const char* marketLabel, const char* agentLabel, libshared_KBoolean agentReady);
                const char* (*get_agentLabel)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                libshared_KBoolean (*get_agentReady)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                const char* (*get_marketLabel)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                libshared_KBoolean (*get_online)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                libshared_KBoolean (*component1)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                libshared_KBoolean (*component4)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                libshared_kref_com_zhiniu_data_remote_GatewayStatus (*copy)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz, libshared_KBoolean online, const char* marketLabel, const char* agentLabel, libshared_KBoolean agentReady);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_data_remote_GatewayStatus thiz);
              } GatewayStatus;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_data_remote_GatewayMarketClient (*_instance)();
                const char* (*get_DEFAULT_BASE_URL)(libshared_kref_com_zhiniu_data_remote_GatewayMarketClient thiz);
                const char* (*get_baseUrl)(libshared_kref_com_zhiniu_data_remote_GatewayMarketClient thiz);
                void (*set_baseUrl)(libshared_kref_com_zhiniu_data_remote_GatewayMarketClient thiz, const char* value);
              } GatewayMarketClient;
            } remote;
          } data;
          struct {
            struct {
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_AiInsight (*AiInsight)(const char* symbol, const char* verdict, const char* trend, const char* volume, const char* indicator, const char* risk, const char* valuation, const char* earnings, libshared_kref_kotlin_collections_List followUps);
                const char* (*get_earnings)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                libshared_kref_kotlin_collections_List (*get_followUps)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*get_indicator)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*get_risk)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*get_symbol)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*get_trend)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*get_valuation)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*get_verdict)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*get_volume)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component4)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component5)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component6)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component7)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*component8)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                libshared_kref_kotlin_collections_List (*component9)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                libshared_kref_com_zhiniu_domain_model_AiInsight (*copy)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz, const char* symbol, const char* verdict, const char* trend, const char* volume, const char* indicator, const char* risk, const char* valuation, const char* earnings, libshared_kref_kotlin_collections_List followUps);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_AiInsight thiz);
              } AiInsight;
              struct {
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_NO_NETWORK (*_instance)();
                } NO_NETWORK;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_RATE_LIMIT (*_instance)();
                } RATE_LIMIT;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_AUTH_INVALID (*_instance)();
                } AUTH_INVALID;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_UPSTREAM_5XX (*_instance)();
                } UPSTREAM_5XX;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_JSON_SCHEMA_FAILED (*_instance)();
                } JSON_SCHEMA_FAILED;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_NIL (*_instance)();
                } NIL;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_Unknown (*Unknown)(const char* overrideDetails);
                } Unknown;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_AppError_Companion (*_instance)();
                  libshared_kref_com_zhiniu_domain_model_AppError (*fromBackendCode)(libshared_kref_com_zhiniu_domain_model_AppError_Companion thiz, const char* code);
                  libshared_kref_com_zhiniu_domain_model_AppError (*fromHttpStatus)(libshared_kref_com_zhiniu_domain_model_AppError_Companion thiz, libshared_KInt status);
                  libshared_kref_com_zhiniu_domain_model_AppError (*fromThrowable)(libshared_kref_com_zhiniu_domain_model_AppError_Companion thiz, libshared_kref_kotlin_Throwable t);
                } Companion;
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_AppError (*AppError)(const char* code, const char* message);
                const char* (*get_code)(libshared_kref_com_zhiniu_domain_model_AppError thiz);
                const char* (*get_message)(libshared_kref_com_zhiniu_domain_model_AppError thiz);
                const char* (*display)(libshared_kref_com_zhiniu_domain_model_AppError thiz);
              } AppError;
              struct {
                struct {
                  libshared_kref_com_zhiniu_domain_model_MessageRole (*get)(); /* enum entry for USER. */
                } USER;
                struct {
                  libshared_kref_com_zhiniu_domain_model_MessageRole (*get)(); /* enum entry for ASSISTANT. */
                } ASSISTANT;
                struct {
                  libshared_kref_com_zhiniu_domain_model_MessageRole (*get)(); /* enum entry for SYSTEM. */
                } SYSTEM;
                struct {
                  libshared_kref_com_zhiniu_domain_model_MessageRole (*get)(); /* enum entry for TOOL. */
                } TOOL;
                libshared_KType* (*_type)(void);
              } MessageRole;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_ChatMessage (*ChatMessage)(const char* id, libshared_kref_com_zhiniu_domain_model_MessageRole role, const char* content, libshared_KBoolean isStreaming);
                const char* (*get_content)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                const char* (*get_id)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                libshared_KBoolean (*get_isStreaming)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                libshared_kref_com_zhiniu_domain_model_MessageRole (*get_role)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                libshared_kref_com_zhiniu_domain_model_MessageRole (*component2)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                libshared_KBoolean (*component4)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                libshared_kref_com_zhiniu_domain_model_ChatMessage (*copy)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz, const char* id, libshared_kref_com_zhiniu_domain_model_MessageRole role, const char* content, libshared_KBoolean isStreaming);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_ChatMessage thiz);
              } ChatMessage;
              struct {
                struct {
                  libshared_kref_com_zhiniu_domain_model_CandleDirection (*get)(); /* enum entry for UP. */
                } UP;
                struct {
                  libshared_kref_com_zhiniu_domain_model_CandleDirection (*get)(); /* enum entry for DOWN. */
                } DOWN;
                struct {
                  libshared_kref_com_zhiniu_domain_model_CandleDirection (*get)(); /* enum entry for FLAT. */
                } FLAT;
                libshared_KType* (*_type)(void);
              } CandleDirection;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_CandleGeometry (*CandleGeometry)(libshared_KFloat x, libshared_KFloat bodyTop, libshared_KFloat bodyBottom, libshared_KFloat bodyWidth, libshared_KFloat wickHigh, libshared_KFloat wickLow, libshared_kref_com_zhiniu_domain_model_CandleDirection direction);
                libshared_KFloat (*get_bodyBottom)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*get_bodyTop)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*get_bodyWidth)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_kref_com_zhiniu_domain_model_CandleDirection (*get_direction)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*get_wickHigh)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*get_wickLow)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*get_x)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*component1)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*component2)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*component3)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*component4)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*component5)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_KFloat (*component6)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_kref_com_zhiniu_domain_model_CandleDirection (*component7)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                libshared_kref_com_zhiniu_domain_model_CandleGeometry (*copy)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz, libshared_KFloat x, libshared_KFloat bodyTop, libshared_KFloat bodyBottom, libshared_KFloat bodyWidth, libshared_KFloat wickHigh, libshared_KFloat wickLow, libshared_kref_com_zhiniu_domain_model_CandleDirection direction);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_CandleGeometry thiz);
              } CandleGeometry;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle (*CandleGeometryBundle)(libshared_kref_kotlin_collections_List candles, libshared_kref_kotlin_collections_List ma5, libshared_kref_kotlin_collections_List ma10, libshared_kref_kotlin_collections_List ma20, libshared_KDouble minPrice, libshared_KDouble maxPrice);
                libshared_kref_kotlin_collections_List (*get_candles)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_kotlin_collections_List (*get_ma10)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_kotlin_collections_List (*get_ma20)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_kotlin_collections_List (*get_ma5)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_KDouble (*get_maxPrice)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_KDouble (*get_minPrice)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_kotlin_collections_List (*component2)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_kotlin_collections_List (*component3)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_kotlin_collections_List (*component4)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_KDouble (*component5)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_KDouble (*component6)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle (*copy)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz, libshared_kref_kotlin_collections_List candles, libshared_kref_kotlin_collections_List ma5, libshared_kref_kotlin_collections_List ma10, libshared_kref_kotlin_collections_List ma20, libshared_KDouble minPrice, libshared_KDouble maxPrice);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle thiz);
              } CandleGeometryBundle;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_KLineChart (*_instance)();
                libshared_kref_com_zhiniu_domain_model_CandleGeometryBundle (*buildBundle)(libshared_kref_com_zhiniu_domain_model_KLineChart thiz, libshared_kref_kotlin_collections_List bars, libshared_KFloat width, libshared_KFloat height);
                libshared_kref_com_zhiniu_domain_model_CandleDirection (*candleDirection)(libshared_kref_com_zhiniu_domain_model_KLineChart thiz, libshared_KDouble open, libshared_KDouble close);
                libshared_kref_kotlin_collections_List (*layout)(libshared_kref_com_zhiniu_domain_model_KLineChart thiz, libshared_kref_kotlin_collections_List bars, libshared_KFloat width, libshared_KFloat height, libshared_KFloat padL, libshared_KFloat padR, libshared_KFloat bodyWidthFactor);
                libshared_kref_kotlin_collections_List (*movingAverage)(libshared_kref_com_zhiniu_domain_model_KLineChart thiz, libshared_kref_kotlin_collections_List closes, libshared_KInt period);
                libshared_kref_kotlin_Pair (*priceBounds)(libshared_kref_com_zhiniu_domain_model_KLineChart thiz, libshared_kref_kotlin_collections_List bars);
              } KLineChart;
              struct {
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_model_SseParser_Frame (*Frame)(const char* event, const char* data);
                  const char* (*get_data)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz);
                  const char* (*get_event)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz);
                  const char* (*component1)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz);
                  const char* (*component2)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz);
                  libshared_kref_com_zhiniu_domain_model_SseParser_Frame (*copy)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz, const char* event, const char* data);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_domain_model_SseParser_Frame thiz);
                } Frame;
                struct {
                  struct {
                    libshared_kref_com_zhiniu_domain_model_SseParser_EventKind (*get)(); /* enum entry for DATA. */
                  } DATA;
                  struct {
                    libshared_kref_com_zhiniu_domain_model_SseParser_EventKind (*get)(); /* enum entry for STEP_PROGRESS. */
                  } STEP_PROGRESS;
                  struct {
                    libshared_kref_com_zhiniu_domain_model_SseParser_EventKind (*get)(); /* enum entry for ERROR. */
                  } ERROR;
                  struct {
                    libshared_kref_com_zhiniu_domain_model_SseParser_EventKind (*get)(); /* enum entry for DONE. */
                  } DONE;
                  libshared_KType* (*_type)(void);
                } EventKind;
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_SseParser (*_instance)();
                libshared_KBoolean (*isDone)(libshared_kref_com_zhiniu_domain_model_SseParser thiz, const char* data);
                libshared_kref_com_zhiniu_domain_model_SseParser_EventKind (*kind)(libshared_kref_com_zhiniu_domain_model_SseParser thiz, const char* event, const char* data);
                libshared_kref_com_zhiniu_domain_model_SseParser_Frame (*parseLine)(libshared_kref_com_zhiniu_domain_model_SseParser thiz, const char* line);
              } SseParser;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_OrderBookLevel (*OrderBookLevel)(libshared_KDouble price, libshared_KLong volumeShares);
                libshared_KDouble (*get_price)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz);
                libshared_KLong (*get_volumeShares)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz);
                libshared_KDouble (*component1)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz);
                libshared_KLong (*component2)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz);
                libshared_kref_com_zhiniu_domain_model_OrderBookLevel (*copy)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz, libshared_KDouble price, libshared_KLong volumeShares);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_OrderBookLevel thiz);
              } OrderBookLevel;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot (*FundFlowSnapshot)(const char* asOf, libshared_kref_kotlin_Double mainNetInflow, libshared_kref_kotlin_Double smallNetInflow, libshared_kref_kotlin_Double mediumNetInflow, libshared_kref_kotlin_Double largeNetInflow, libshared_kref_kotlin_Double superLargeNetInflow);
                const char* (*get_asOf)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*get_largeNetInflow)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*get_mainNetInflow)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*get_mediumNetInflow)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*get_smallNetInflow)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*get_superLargeNetInflow)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*component2)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*component3)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*component4)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*component5)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_kotlin_Double (*component6)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot (*copy)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz, const char* asOf, libshared_kref_kotlin_Double mainNetInflow, libshared_kref_kotlin_Double smallNetInflow, libshared_kref_kotlin_Double mediumNetInflow, libshared_kref_kotlin_Double largeNetInflow, libshared_kref_kotlin_Double superLargeNetInflow);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot thiz);
              } FundFlowSnapshot;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_StockFundamentals (*StockFundamentals)(const char* symbol, const char* name, const char* industry, const char* region, libshared_kref_kotlin_collections_List concepts, libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double turnoverRate, libshared_kref_kotlin_Double amplitude, libshared_kref_kotlin_Double marketCap, libshared_kref_kotlin_Double floatMarketCap, const char* reportDate, const char* reportType, libshared_kref_kotlin_Double revenue, libshared_kref_kotlin_Double netProfit, libshared_kref_kotlin_Double roe, libshared_kref_kotlin_Double grossMargin, libshared_kref_kotlin_Double revenueYoY, libshared_kref_kotlin_Double netProfitYoY, libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot moneyFlow, const char* source, const char* provider, libshared_KBoolean isStale, libshared_KBoolean available);
                libshared_kref_kotlin_Double (*get_amplitude)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_KBoolean (*get_available)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_collections_List (*get_concepts)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_floatMarketCap)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_grossMargin)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_industry)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_KBoolean (*get_isStale)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_marketCap)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot (*get_moneyFlow)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_name)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_netProfit)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_netProfitYoY)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_pb)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_pe)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_provider)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_region)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_reportDate)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_reportType)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_revenue)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_revenueYoY)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_roe)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*get_symbol)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*get_turnoverRate)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component10)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component11)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component12)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component13)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component14)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component15)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component16)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component17)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component18)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component19)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot (*component20)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component21)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component22)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_KBoolean (*component23)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_KBoolean (*component24)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*component4)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_collections_List (*component5)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component6)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component7)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component8)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_kotlin_Double (*component9)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                libshared_kref_com_zhiniu_domain_model_StockFundamentals (*copy)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz, const char* symbol, const char* name, const char* industry, const char* region, libshared_kref_kotlin_collections_List concepts, libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double turnoverRate, libshared_kref_kotlin_Double amplitude, libshared_kref_kotlin_Double marketCap, libshared_kref_kotlin_Double floatMarketCap, const char* reportDate, const char* reportType, libshared_kref_kotlin_Double revenue, libshared_kref_kotlin_Double netProfit, libshared_kref_kotlin_Double roe, libshared_kref_kotlin_Double grossMargin, libshared_kref_kotlin_Double revenueYoY, libshared_kref_kotlin_Double netProfitYoY, libshared_kref_com_zhiniu_domain_model_FundFlowSnapshot moneyFlow, const char* source, const char* provider, libshared_KBoolean isStale, libshared_KBoolean available);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_StockFundamentals thiz);
              } StockFundamentals;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_StockQuote (*StockQuote)(const char* symbol, const char* name, const char* pinyin, libshared_KDouble open, libshared_KDouble prevClose, libshared_KDouble price, libshared_KDouble high, libshared_KDouble low, libshared_KDouble buy1, libshared_KDouble sell1, libshared_kref_kotlin_collections_List bids, libshared_kref_kotlin_collections_List asks, libshared_KLong volume, libshared_KDouble amount, libshared_kref_kotlin_Double marketCap, libshared_kref_kotlin_Double turnoverRate, libshared_kref_kotlin_Double volumeRatio, const char* date, const char* time, const char* source, const char* provider, libshared_KBoolean isStale);
                libshared_KDouble (*get_amount)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_collections_List (*get_asks)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_collections_List (*get_bids)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_buy1)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_change)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_changePercent)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_code)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_date)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_high)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KBoolean (*get_isStale)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KBoolean (*get_isUp)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_low)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_Double (*get_marketCap)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_marketSuffix)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_name)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_open)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_pinyin)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_prevClose)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_price)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_provider)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*get_sell1)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_source)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_symbol)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*get_time)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_Double (*get_turnoverRate)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KLong (*get_volume)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_Double (*get_volumeRatio)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component10)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_collections_List (*component11)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_collections_List (*component12)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KLong (*component13)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component14)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_Double (*component15)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_Double (*component16)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_kotlin_Double (*component17)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*component18)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*component19)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*component20)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*component21)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KBoolean (*component22)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component4)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component5)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component6)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component7)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component8)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_KDouble (*component9)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                libshared_kref_com_zhiniu_domain_model_StockQuote (*copy)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz, const char* symbol, const char* name, const char* pinyin, libshared_KDouble open, libshared_KDouble prevClose, libshared_KDouble price, libshared_KDouble high, libshared_KDouble low, libshared_KDouble buy1, libshared_KDouble sell1, libshared_kref_kotlin_collections_List bids, libshared_kref_kotlin_collections_List asks, libshared_KLong volume, libshared_KDouble amount, libshared_kref_kotlin_Double marketCap, libshared_kref_kotlin_Double turnoverRate, libshared_kref_kotlin_Double volumeRatio, const char* date, const char* time, const char* source, const char* provider, libshared_KBoolean isStale);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_StockQuote thiz);
              } StockQuote;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_MarketIndex (*MarketIndex)(const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent, libshared_kref_kotlin_collections_List spark);
                libshared_KDouble (*get_changePercent)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                libshared_KBoolean (*get_isUp)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                const char* (*get_name)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                libshared_KDouble (*get_price)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                libshared_kref_kotlin_collections_List (*get_spark)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                const char* (*get_symbol)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                libshared_KDouble (*component3)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                libshared_KDouble (*component4)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                libshared_kref_kotlin_collections_List (*component5)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                libshared_kref_com_zhiniu_domain_model_MarketIndex (*copy)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz, const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent, libshared_kref_kotlin_collections_List spark);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_MarketIndex thiz);
              } MarketIndex;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_MarketBreadth (*MarketBreadth)(libshared_KLong upCount, libshared_KLong downCount, libshared_KLong amountYi, const char* status);
                libshared_KLong (*get_amountYi)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_KLong (*get_downCount)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                const char* (*get_status)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_KLong (*get_total)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_KLong (*get_upCount)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_KDouble (*get_upRatio)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_KLong (*component1)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_KLong (*component2)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_KLong (*component3)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                const char* (*component4)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                libshared_kref_com_zhiniu_domain_model_MarketBreadth (*copy)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz, libshared_KLong upCount, libshared_KLong downCount, libshared_KLong amountYi, const char* status);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_MarketBreadth thiz);
              } MarketBreadth;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_Candle (*Candle)(const char* day, libshared_KDouble open, libshared_KDouble high, libshared_KDouble low, libshared_KDouble close, libshared_KLong volume);
                libshared_KDouble (*get_close)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                const char* (*get_day)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KDouble (*get_high)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KDouble (*get_low)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KDouble (*get_open)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KLong (*get_volume)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KDouble (*component2)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KDouble (*component3)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KDouble (*component4)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KDouble (*component5)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_KLong (*component6)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                libshared_kref_com_zhiniu_domain_model_Candle (*copy)(libshared_kref_com_zhiniu_domain_model_Candle thiz, const char* day, libshared_KDouble open, libshared_KDouble high, libshared_KDouble low, libshared_KDouble close, libshared_KLong volume);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_Candle thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_Candle thiz);
              } Candle;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_TechnicalIndicator (*TechnicalIndicator)(const char* name, libshared_KDouble value, const char* detail);
                const char* (*get_detail)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
                const char* (*get_name)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
                libshared_KDouble (*get_value)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
                libshared_KDouble (*component2)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
                libshared_kref_com_zhiniu_domain_model_TechnicalIndicator (*copy)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz, const char* name, libshared_KDouble value, const char* detail);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_TechnicalIndicator thiz);
              } TechnicalIndicator;
              struct {
                struct {
                  libshared_kref_com_zhiniu_domain_model_Timeframe (*get)(); /* enum entry for INTRADAY. */
                } INTRADAY;
                struct {
                  libshared_kref_com_zhiniu_domain_model_Timeframe (*get)(); /* enum entry for DAY. */
                } DAY;
                struct {
                  libshared_kref_com_zhiniu_domain_model_Timeframe (*get)(); /* enum entry for WEEK. */
                } WEEK;
                struct {
                  libshared_kref_com_zhiniu_domain_model_Timeframe (*get)(); /* enum entry for MONTH. */
                } MONTH;
                libshared_KType* (*_type)(void);
                const char* (*get_label)(libshared_kref_com_zhiniu_domain_model_Timeframe thiz);
              } Timeframe;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_ChatSession (*ChatSession)(const char* id, const char* title, const char* createdAt);
                const char* (*get_createdAt)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
                const char* (*get_id)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
                const char* (*get_title)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
                const char* (*component3)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
                libshared_kref_com_zhiniu_domain_model_ChatSession (*copy)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz, const char* id, const char* title, const char* createdAt);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_ChatSession thiz);
              } ChatSession;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_TodoItem (*TodoItem)(libshared_KLong id, const char* title, libshared_KBoolean done, libshared_KLong createdAt);
                libshared_KLong (*get_createdAt)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                libshared_KBoolean (*get_done)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                libshared_KLong (*get_id)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                const char* (*get_title)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                libshared_KLong (*component1)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                libshared_KBoolean (*component3)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                libshared_KLong (*component4)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                libshared_kref_com_zhiniu_domain_model_TodoItem (*copy)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz, libshared_KLong id, const char* title, libshared_KBoolean done, libshared_KLong createdAt);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_model_TodoItem thiz);
              } TodoItem;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_TodoStore (*_instance)();
                libshared_kref_kotlin_collections_List (*add)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, libshared_kref_kotlin_collections_List items, const char* title, libshared_KLong nowMs);
                libshared_kref_kotlin_collections_List (*clearDone)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, libshared_kref_kotlin_collections_List items);
                libshared_kref_kotlin_collections_List (*deserialize)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, const char* raw);
                libshared_KLong (*nextId)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, libshared_kref_kotlin_collections_List items);
                const char* (*normalizeTitle)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, const char* raw);
                libshared_kref_kotlin_collections_List (*remove)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, libshared_kref_kotlin_collections_List items, libshared_KLong id);
                libshared_kref_kotlin_collections_List (*rename)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, libshared_kref_kotlin_collections_List items, libshared_KLong id, const char* title);
                const char* (*serialize)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, libshared_kref_kotlin_collections_List items);
                libshared_kref_kotlin_collections_List (*toggle)(libshared_kref_com_zhiniu_domain_model_TodoStore thiz, libshared_kref_kotlin_collections_List items, libshared_KLong id);
              } TodoStore;
            } model;
            struct {
              struct {
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_Text (*Text)(const char* content);
                  const char* (*get_content)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Text thiz);
                  const char* (*component1)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Text thiz);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_Text (*copy)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Text thiz, const char* content);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Text thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Text thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Text thiz);
                } Text;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard (*StockCard)(const char* symbol, const char* trend, libshared_KDouble rsi, const char* summary);
                  libshared_KDouble (*get_rsi)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  const char* (*get_summary)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  const char* (*get_symbol)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  const char* (*get_trend)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  const char* (*component1)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  const char* (*component2)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  libshared_KDouble (*component3)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  const char* (*component4)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard (*copy)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz, const char* symbol, const char* trend, libshared_KDouble rsi, const char* summary);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_domain_repository_AiBlock_StockCard thiz);
                } StockCard;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics (*Metrics)(const char* title, libshared_kref_kotlin_collections_List rows);
                  libshared_kref_kotlin_collections_List (*get_rows)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz);
                  const char* (*get_title)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz);
                  const char* (*component1)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz);
                  libshared_kref_kotlin_collections_List (*component2)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics (*copy)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz, const char* title, libshared_kref_kotlin_collections_List rows);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Metrics thiz);
                } Metrics;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk (*Risk)(const char* title, const char* content);
                  const char* (*get_content)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz);
                  const char* (*get_title)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz);
                  const char* (*component1)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz);
                  const char* (*component2)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk (*copy)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz, const char* title, const char* content);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_domain_repository_AiBlock_Risk thiz);
                } Risk;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps (*FollowUps)(libshared_kref_kotlin_collections_List questions);
                  libshared_kref_kotlin_collections_List (*get_questions)(libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps thiz);
                  libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps thiz);
                  libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps (*copy)(libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps thiz, libshared_kref_kotlin_collections_List questions);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_domain_repository_AiBlock_FollowUps thiz);
                } FollowUps;
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_repository_AiBlock (*AiBlock)();
              } AiBlock;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_repository_MetricCell (*MetricCell)(const char* label, const char* value);
                const char* (*get_label)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz);
                const char* (*get_value)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz);
                const char* (*component1)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz);
                const char* (*component2)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz);
                libshared_kref_com_zhiniu_domain_repository_MetricCell (*copy)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz, const char* label, const char* value);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_repository_MetricCell thiz);
              } MetricCell;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_kotlin_collections_List (*chatReply)(libshared_kref_com_zhiniu_domain_repository_AiService thiz, const char* sessionId, const char* question);
                libshared_kref_com_zhiniu_domain_model_AiInsight (*insightFor)(libshared_kref_com_zhiniu_domain_repository_AiService thiz, const char* symbol, libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals fundamentals);
              } AiService;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals (*AiInsightFundamentals)(libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double marketCap, const char* reportDate, libshared_kref_kotlin_Double revenue, libshared_kref_kotlin_Double netProfit, libshared_kref_kotlin_Double roe, libshared_kref_kotlin_Double grossMargin);
                libshared_kref_kotlin_Double (*get_grossMargin)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*get_marketCap)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*get_netProfit)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*get_pb)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*get_pe)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                const char* (*get_reportDate)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*get_revenue)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*get_roe)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*component1)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*component2)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*component3)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                const char* (*component4)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*component5)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*component6)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*component7)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_kotlin_Double (*component8)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals (*copy)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz, libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double marketCap, const char* reportDate, libshared_kref_kotlin_Double revenue, libshared_kref_kotlin_Double netProfit, libshared_kref_kotlin_Double roe, libshared_kref_kotlin_Double grossMargin);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_domain_repository_AiInsightFundamentals thiz);
              } AiInsightFundamentals;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_domain_model_MarketBreadth (*breadth)(libshared_kref_com_zhiniu_domain_repository_MarketRepository thiz);
                libshared_kref_kotlin_collections_List (*candles)(libshared_kref_com_zhiniu_domain_repository_MarketRepository thiz, const char* symbol, libshared_kref_com_zhiniu_domain_model_Timeframe timeframe);
                libshared_kref_kotlin_collections_List (*indices)(libshared_kref_com_zhiniu_domain_repository_MarketRepository thiz);
                libshared_kref_com_zhiniu_domain_model_StockQuote (*quoteOf)(libshared_kref_com_zhiniu_domain_repository_MarketRepository thiz, const char* symbol);
                libshared_kref_kotlin_collections_List (*search)(libshared_kref_com_zhiniu_domain_repository_MarketRepository thiz, const char* query);
                libshared_kref_kotlin_collections_List (*spark)(libshared_kref_com_zhiniu_domain_repository_MarketRepository thiz, const char* symbol);
                libshared_kref_kotlin_collections_List (*stockQuotes)(libshared_kref_com_zhiniu_domain_repository_MarketRepository thiz);
              } MarketRepository;
            } repository;
          } domain;
          struct {
            struct {
              libshared_KType* (*_type)(void);
              libshared_kref_com_zhiniu_pages_AiChatMessage (*AiChatMessage)(const char* role, const char* text, libshared_kref_kotlin_collections_List blocks, libshared_KBoolean streaming, libshared_kref_kotlin_collections_List progress, const char* requestId);
              libshared_kref_kotlin_collections_List (*get_blocks)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              libshared_kref_kotlin_collections_List (*get_progress)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              const char* (*get_requestId)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              const char* (*get_role)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              libshared_KBoolean (*get_streaming)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              const char* (*get_text)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              const char* (*component1)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              const char* (*component2)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              libshared_kref_kotlin_collections_List (*component3)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              libshared_KBoolean (*component4)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              libshared_kref_kotlin_collections_List (*component5)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              const char* (*component6)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              libshared_kref_com_zhiniu_pages_AiChatMessage (*copy)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz, const char* role, const char* text, libshared_kref_kotlin_collections_List blocks, libshared_KBoolean streaming, libshared_kref_kotlin_collections_List progress, const char* requestId);
              libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz, libshared_kref_kotlin_Any other);
              libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
              const char* (*toString)(libshared_kref_com_zhiniu_pages_AiChatMessage thiz);
            } AiChatMessage;
            struct {
              struct {
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine (*AiPanelChatLine)(const char* role, const char* text);
                  const char* (*get_role)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz);
                  const char* (*get_text)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz);
                  const char* (*component1)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz);
                  const char* (*component2)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz);
                  libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine (*copy)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz, const char* role, const char* text);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_AiPanelChatLine thiz);
                } AiPanelChatLine;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_pages_components_ai_MdSpan (*MdSpan)(const char* text, libshared_KBoolean bold, libshared_KBoolean code);
                  libshared_KBoolean (*get_bold)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                  libshared_KBoolean (*get_code)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                  const char* (*get_text)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                  const char* (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                  libshared_KBoolean (*component2)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                  libshared_KBoolean (*component3)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                  libshared_kref_com_zhiniu_pages_components_ai_MdSpan (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz, const char* text, libshared_KBoolean bold, libshared_KBoolean code);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdSpan thiz);
                } MdSpan;
                struct {
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading (*Heading)(libshared_KInt level, libshared_kref_kotlin_collections_List spans);
                    libshared_KInt (*get_level)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz);
                    libshared_kref_kotlin_collections_List (*get_spans)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz);
                    libshared_KInt (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz);
                    libshared_kref_kotlin_collections_List (*component2)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz, libshared_KInt level, libshared_kref_kotlin_collections_List spans);
                    libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz, libshared_kref_kotlin_Any other);
                    libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz);
                    const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Heading thiz);
                  } Heading;
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph (*Paragraph)(libshared_kref_kotlin_collections_List spans);
                    libshared_kref_kotlin_collections_List (*get_spans)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph thiz);
                    libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph thiz);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph thiz, libshared_kref_kotlin_collections_List spans);
                    libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph thiz, libshared_kref_kotlin_Any other);
                    libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph thiz);
                    const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Paragraph thiz);
                  } Paragraph;
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets (*Bullets)(libshared_kref_kotlin_collections_List items);
                    libshared_kref_kotlin_collections_List (*get_items)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets thiz);
                    libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets thiz);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets thiz, libshared_kref_kotlin_collections_List items);
                    libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets thiz, libshared_kref_kotlin_Any other);
                    libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets thiz);
                    const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Bullets thiz);
                  } Bullets;
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList (*OrderedList)(libshared_kref_kotlin_collections_List items);
                    libshared_kref_kotlin_collections_List (*get_items)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList thiz);
                    libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList thiz);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList thiz, libshared_kref_kotlin_collections_List items);
                    libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList thiz, libshared_kref_kotlin_Any other);
                    libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList thiz);
                    const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_OrderedList thiz);
                  } OrderedList;
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote (*Quote)(libshared_kref_kotlin_collections_List spans);
                    libshared_kref_kotlin_collections_List (*get_spans)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote thiz);
                    libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote thiz);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote thiz, libshared_kref_kotlin_collections_List spans);
                    libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote thiz, libshared_kref_kotlin_Any other);
                    libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote thiz);
                    const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Quote thiz);
                  } Quote;
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code (*Code)(const char* language, libshared_kref_kotlin_collections_List lines);
                    const char* (*get_language)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz);
                    libshared_kref_kotlin_collections_List (*get_lines)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz);
                    const char* (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz);
                    libshared_kref_kotlin_collections_List (*component2)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz, const char* language, libshared_kref_kotlin_collections_List lines);
                    libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz, libshared_kref_kotlin_Any other);
                    libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz);
                    const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Code thiz);
                  } Code;
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table (*Table)(libshared_kref_kotlin_collections_List header, libshared_kref_kotlin_collections_List rows);
                    libshared_kref_kotlin_collections_List (*get_header)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz);
                    libshared_kref_kotlin_collections_List (*get_rows)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz);
                    libshared_kref_kotlin_collections_List (*component1)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz);
                    libshared_kref_kotlin_collections_List (*component2)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table (*copy)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz, libshared_kref_kotlin_collections_List header, libshared_kref_kotlin_collections_List rows);
                    libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz, libshared_kref_kotlin_Any other);
                    libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz);
                    const char* (*toString)(libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Table thiz);
                  } Table;
                  struct {
                    libshared_KType* (*_type)(void);
                    libshared_kref_com_zhiniu_pages_components_ai_MdBlock_Divider (*_instance)();
                  } Divider;
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_pages_components_ai_MdBlock (*MdBlock)();
                } MdBlock;
                void (*AiBlockView)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_com_zhiniu_domain_repository_AiBlock block, libshared_kref_kotlin_Function1 onOpenStock, libshared_kref_kotlin_Function1 onAsk);
                void (*AiStockCard)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* symbol, const char* trend, libshared_KDouble rsi, const char* summary, libshared_kref_kotlin_Function0 onClick);
                void (*AiInsightPanel)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_KFloat width, libshared_KFloat height, libshared_kref_kotlin_Function0 quote, libshared_kref_kotlin_Function0 insight, libshared_kref_kotlin_Function0 followUpText, libshared_kref_kotlin_Function0 chatLines, libshared_kref_kotlin_Function1 onFollowUpChange, libshared_kref_kotlin_Function1 onFollowUpSend, libshared_kref_kotlin_Function0 onClose);
                libshared_kref_kotlin_collections_List (*parseInlineSpans)(const char* s);
                libshared_kref_kotlin_collections_List (*parseMarkdown)(const char* src);
                void (*MarkdownView)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* content);
              } ai;
              struct {
                struct {
                  struct {
                    libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator (*get)(); /* enum entry for NONE. */
                  } NONE;
                  struct {
                    libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator (*get)(); /* enum entry for MA. */
                  } MA;
                  struct {
                    libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator (*get)(); /* enum entry for MACD. */
                  } MACD;
                  struct {
                    libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator (*get)(); /* enum entry for RSI. */
                  } RSI;
                  libshared_KType* (*_type)(void);
                } ChartIndicator;
                libshared_kref_kotlin_collections_List (*get_INDICATORS)();
                libshared_kref_kotlin_collections_List (*get_TIMEFRAMES)();
                libshared_kref_kotlin_Double (*maValue)(libshared_kref_kotlin_collections_List bars, libshared_KInt n);
                libshared_kref_kotlin_Double (*rsiNow)(libshared_kref_kotlin_collections_List bars);
                void (*ChartToolbar)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_KBoolean compact, libshared_kref_kotlin_Function0 timeframe, libshared_kref_kotlin_Function0 indicator, libshared_kref_kotlin_Function0 bars, libshared_kref_kotlin_Function1 onTimeframe, libshared_kref_kotlin_Function1 onIndicator, libshared_kref_kotlin_Function0 visibleCount, libshared_kref_kotlin_Function0 onZoomIn, libshared_kref_kotlin_Function0 onZoomOut, libshared_kref_kotlin_Function0 onResetZoom);
                libshared_KInt (*autoVisibleCount)(libshared_KInt total);
                libshared_KInt (*clampViewStart)(libshared_KInt viewStart, libshared_KInt total, libshared_KInt viewCount);
                void (*drawKLineChart)(libshared_kref_com_tencent_kuikly_core_views_CanvasContext context, libshared_KFloat w, libshared_KFloat h, libshared_kref_kotlin_collections_List bars, libshared_kref_com_zhiniu_pages_components_AppColors colors, libshared_kref_com_zhiniu_pages_components_chart_ChartIndicator indicator, libshared_KFloat crossX, libshared_KFloat crossY, libshared_KBoolean intraday, libshared_KInt viewStart, libshared_KInt viewCount);
                libshared_kref_kotlin_Triple (*macd)(libshared_kref_kotlin_collections_List values);
                libshared_kref_kotlin_collections_List (*rsi)(libshared_kref_kotlin_collections_List values, libshared_KInt n);
              } chart;
              struct {
                void (*ActivityIndicatorRow)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* label);
                void (*AppCard)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_KFloat padding, libshared_kref_kotlin_Function1 content);
                void (*Divider)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz);
                void (*EmptyState)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* title, const char* desc, const char* actionLabel, libshared_kref_kotlin_Function0 onAction);
                void (*QuoteMetric)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* label, const char* value);
                void (*SectionHeader)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* title, const char* action, libshared_kref_kotlin_Function0 onAction);
                void (*SkeletonBar)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_KFloat width, libshared_KFloat height);
                libshared_KFloat (*get_BOTTOM_TAB_HEIGHT)();
                void (*BottomTabBar)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* activeNav, libshared_KFloat bottomInset, libshared_kref_kotlin_Function0 onNavMarket, libshared_kref_kotlin_Function0 onNavAiResearch, libshared_kref_kotlin_Function0 onNavTodo);
                void (*FavoriteButton)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_Function0 active, libshared_KFloat height, libshared_kref_kotlin_Function0 onToggle);
                void (*GhostButton)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* label, libshared_KFloat height, libshared_kref_com_zhiniu_pages_components_IconKind icon, libshared_kref_kotlin_Function0 onClick);
                void (*IconButton)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_com_zhiniu_pages_components_IconKind kind, libshared_KFloat size, libshared_KFloat box, libshared_KBoolean active, const char* accessibilityLabel, libshared_kref_kotlin_Function0 onClick);
                void (*PrimaryButton)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* label, libshared_KFloat height, libshared_kref_com_zhiniu_pages_components_IconKind icon, libshared_kref_kotlin_Function0 onClick);
                void (*SecondaryButton)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* label, libshared_KFloat height, libshared_kref_com_zhiniu_pages_components_IconKind icon, libshared_kref_kotlin_Function0 onClick);
                void (*AppHeader)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* activeNav, libshared_KFloat contentWidth, libshared_KFloat topInset, libshared_kref_kotlin_Function0 onNavMarket, libshared_kref_kotlin_Function0 onNavAiResearch, libshared_kref_kotlin_Function0 onNavTodo, libshared_kref_kotlin_Function0 onSearch, libshared_kref_kotlin_Function0 onTheme);
                void (*AppInput)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* placeholder, const char* text, libshared_KFloat height, libshared_kref_kotlin_Function1 onTextChange, libshared_kref_kotlin_Function0 onReturn, libshared_kref_kotlin_Function1 onRef);
                void (*SearchField)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_KFloat width, libshared_kref_kotlin_Function0 onClick);
                void (*StockSearchOverlay)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_Function0 visible, libshared_kref_kotlin_Function0 query, libshared_kref_kotlin_Function0 recent, libshared_kref_kotlin_Function0 hot, libshared_kref_kotlin_Function0 results, libshared_KFloat left, libshared_KFloat width, libshared_KFloat topInset, libshared_kref_kotlin_Function1 onQueryChange, libshared_kref_kotlin_Function1 onPick, libshared_kref_kotlin_Function0 onClose);
                void (*AppTabs)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_collections_List tabs, libshared_kref_kotlin_Function0 selected, libshared_KFloat itemWidth, libshared_KFloat height, libshared_kref_kotlin_Function1 onSelect);
                void (*SegmentedTabs)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_collections_List options, libshared_kref_kotlin_Function0 selected, libshared_KFloat height, libshared_kref_kotlin_Function1 onSelect);
                void (*ThemePopover)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_Function0 visible, libshared_KFloat left, libshared_KFloat width, libshared_KFloat topInset, libshared_kref_kotlin_Function0 gatewayOnline, libshared_kref_kotlin_Function0 agentReady, libshared_kref_kotlin_Function0 onClose);
              } common;
              struct {
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_pages_components_market_StockColumns (*_instance)();
                  libshared_kref_kotlin_collections_List (*get_ALL)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                  const char* (*get_AMOUNT)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                  const char* (*get_CHANGE)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                  libshared_kref_kotlin_collections_Set (*get_DEFAULT)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                  const char* (*get_HIGH_LOW)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                  const char* (*get_MARKET_CAP)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                  const char* (*get_SPARK)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                  const char* (*get_VOLUME)(libshared_kref_com_zhiniu_pages_components_market_StockColumns thiz);
                } StockColumns;
                struct {
                  libshared_KType* (*_type)(void);
                  libshared_kref_com_zhiniu_pages_components_market_RankRow (*RankRow)(libshared_KInt rank, const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent);
                  libshared_KDouble (*get_changePercent)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  libshared_KBoolean (*get_isUp)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  const char* (*get_name)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  libshared_KDouble (*get_price)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  libshared_KInt (*get_rank)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  const char* (*get_symbol)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  libshared_KInt (*component1)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  const char* (*component2)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  const char* (*component3)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  libshared_KDouble (*component4)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  libshared_KDouble (*component5)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  libshared_kref_com_zhiniu_pages_components_market_RankRow (*copy)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz, libshared_KInt rank, const char* symbol, const char* name, libshared_KDouble price, libshared_KDouble changePercent);
                  libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz, libshared_kref_kotlin_Any other);
                  libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                  const char* (*toString)(libshared_kref_com_zhiniu_pages_components_market_RankRow thiz);
                } RankRow;
                void (*Level2Panel)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_com_zhiniu_domain_model_StockQuote q);
                void (*MarketPulse)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_collections_List indices, libshared_kref_com_zhiniu_domain_model_MarketBreadth breadth, libshared_KBoolean narrow, libshared_KBoolean compact);
                void (*RankTable)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_Function0 rows, libshared_KBoolean compact, libshared_kref_kotlin_Function1 onRowClick);
                void (*SectorTable)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_Function0 rows, libshared_KBoolean compact, libshared_kref_kotlin_Function1 onRowClick);
                void (*StockRow)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_com_zhiniu_domain_model_StockQuote q, libshared_kref_kotlin_collections_List spark, libshared_KBoolean narrow, libshared_KBoolean compact, libshared_kref_kotlin_collections_Set columns, libshared_kref_kotlin_Function0 onClick);
                void (*StockTable)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_kotlin_Function0 marketQuotes, libshared_kref_kotlin_Function1 sparkOf, libshared_KBoolean narrow, libshared_KBoolean compact, libshared_kref_kotlin_collections_Set columns, libshared_kref_kotlin_Function1 onRowClick);
                void (*StockTableHeader)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_KBoolean narrow, libshared_KBoolean compact, libshared_kref_kotlin_collections_Set columns);
              } market;
              struct {
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for SEARCH. */
                } SEARCH;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for BACK. */
                } BACK;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for STAR. */
                } STAR;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for STAR_FILLED. */
                } STAR_FILLED;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for FILTER. */
                } FILTER;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for THEME. */
                } THEME;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CLOSE. */
                } CLOSE;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for SEND. */
                } SEND;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for AI. */
                } AI;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for MORE. */
                } MORE;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CHART. */
                } CHART;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CHEVRON_DOWN. */
                } CHEVRON_DOWN;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CHEVRON_UP. */
                } CHEVRON_UP;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CHEVRON_RIGHT. */
                } CHEVRON_RIGHT;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CALENDAR. */
                } CALENDAR;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CHECK. */
                } CHECK;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for FILTER_SORT. */
                } FILTER_SORT;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for ERROR. */
                } ERROR;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for CHAT. */
                } CHAT;
                struct {
                  libshared_kref_com_zhiniu_pages_components_IconKind (*get)(); /* enum entry for DATA. */
                } DATA;
                libshared_KType* (*_type)(void);
                const char* (*get_asset)(libshared_kref_com_zhiniu_pages_components_IconKind thiz);
              } IconKind;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_StockFacts (*StockFacts)(libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double marketCap, libshared_kref_kotlin_Double turnover, libshared_kref_kotlin_Double volumeRatio);
                libshared_kref_kotlin_Double (*get_marketCap)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*get_pb)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*get_pe)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*get_turnover)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*get_volumeRatio)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*component1)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*component2)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*component3)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*component4)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_kotlin_Double (*component5)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                libshared_kref_com_zhiniu_pages_components_StockFacts (*copy)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz, libshared_kref_kotlin_Double pe, libshared_kref_kotlin_Double pb, libshared_kref_kotlin_Double marketCap, libshared_kref_kotlin_Double turnover, libshared_kref_kotlin_Double volumeRatio);
                libshared_KBoolean (*equals)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz, libshared_kref_kotlin_Any other);
                libshared_KInt (*hashCode)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
                const char* (*toString)(libshared_kref_com_zhiniu_pages_components_StockFacts thiz);
              } StockFacts;
              struct {
                struct {
                  libshared_kref_com_zhiniu_pages_components_ThemeMode (*get)(); /* enum entry for SYSTEM. */
                } SYSTEM;
                struct {
                  libshared_kref_com_zhiniu_pages_components_ThemeMode (*get)(); /* enum entry for LIGHT. */
                } LIGHT;
                struct {
                  libshared_kref_com_zhiniu_pages_components_ThemeMode (*get)(); /* enum entry for DARK. */
                } DARK;
                libshared_KType* (*_type)(void);
                const char* (*get_label)(libshared_kref_com_zhiniu_pages_components_ThemeMode thiz);
              } ThemeMode;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_AppTypography (*_instance)();
                libshared_KFloat (*get_fs11)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs12)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs13)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs14)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs15)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs16)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs18)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs20)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs24)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs28)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
                libshared_KFloat (*get_fs32)(libshared_kref_com_zhiniu_pages_components_AppTypography thiz);
              } AppTypography;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_AppSpacing (*_instance)();
                libshared_KFloat (*get_chartHeight)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_header)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_marketTitle)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_pulse)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_row64)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s1)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s10)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s2)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s3)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s4)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s5)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s6)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
                libshared_KFloat (*get_s8)(libshared_kref_com_zhiniu_pages_components_AppSpacing thiz);
              } AppSpacing;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_AppRadius (*_instance)();
                libshared_KFloat (*get_radius5)(libshared_kref_com_zhiniu_pages_components_AppRadius thiz);
                libshared_KFloat (*get_radius6)(libshared_kref_com_zhiniu_pages_components_AppRadius thiz);
                libshared_KFloat (*get_radius8)(libshared_kref_com_zhiniu_pages_components_AppRadius thiz);
              } AppRadius;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_AppColors (*AppColors)(const char* pageBg, const char* surface, const char* surfaceSecondary, const char* surfaceHover, const char* elevated, const char* border, const char* borderStrong, const char* textPrimary, const char* textSecondary, const char* textTertiary, const char* up, const char* down, const char* flat, const char* ma5, const char* ma10, const char* ma20, const char* dif, const char* dea, const char* rsi, const char* chartBg, const char* chartGrid, const char* axisText, const char* crosshair, const char* aiAccent, const char* surfaceRaised);
                const char* (*get_aiAccent)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_axisText)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_border)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_borderStrong)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_chartBg)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_chartGrid)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_crosshair)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_dea)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_dif)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_down)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_elevated)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_flat)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_ma10)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_ma20)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_ma5)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_pageBg)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_rsi)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_surface)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_surfaceHover)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_surfaceRaised)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_surfaceSecondary)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_textPrimary)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_textSecondary)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_textTertiary)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
                const char* (*get_up)(libshared_kref_com_zhiniu_pages_components_AppColors thiz);
              } AppColors;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_LightColors (*_instance)();
              } LightColors;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_DarkColors (*_instance)();
              } DarkColors;
              struct {
                libshared_KType* (*_type)(void);
                libshared_kref_com_zhiniu_pages_components_AppTheme (*_instance)();
                libshared_kref_com_zhiniu_pages_components_AppColors (*get_colors)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz);
                libshared_KBoolean (*get_isDark)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz);
                void (*set_isDark)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz, libshared_KBoolean set);
                libshared_kref_com_zhiniu_pages_components_ThemeMode (*get_mode)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz);
                void (*set_mode)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz, libshared_kref_com_zhiniu_pages_components_ThemeMode set);
                void (*applyMode)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz, libshared_kref_com_zhiniu_pages_components_ThemeMode m);
                libshared_KBoolean (*resolve)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz, libshared_kref_com_zhiniu_pages_components_ThemeMode m);
                void (*start)(libshared_kref_com_zhiniu_pages_components_AppTheme thiz);
              } AppTheme;
              void (*AiMessageHeader)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, const char* label);
              void (*drawSparkLine)(libshared_kref_com_tencent_kuikly_core_views_CanvasContext context, libshared_kref_kotlin_collections_List values, libshared_kref_com_tencent_kuikly_core_base_Color color, libshared_KFloat w, libshared_KFloat h, libshared_KBoolean endDot);
              const char* (*get_NUM_FONT)();
              const char* (*fmt2)(libshared_KDouble v);
              const char* (*fmtAmount)(libshared_KDouble v);
              const char* (*fmtAmplitude)(libshared_KDouble high, libshared_KDouble low, libshared_KDouble prevClose);
              const char* (*fmtChangeSigned)(libshared_KDouble v);
              const char* (*fmtInt)(libshared_KLong v);
              const char* (*fmtMarketCap)(libshared_kref_kotlin_Double v);
              const char* (*fmtPct)(libshared_KDouble v);
              const char* (*fmtSymbol)(const char* sym);
              const char* (*fmtVolHand)(libshared_KLong v);
              const char* (*marketLabelOf)(const char* sym);
              void (*Icon)(libshared_kref_com_tencent_kuikly_core_base_ViewContainer thiz, libshared_kref_com_zhiniu_pages_components_IconKind kind, libshared_KFloat size);
              libshared_kref_com_zhiniu_pages_components_StockFacts (*factsOf)(libshared_kref_com_zhiniu_domain_model_StockFundamentals fundamentals);
              libshared_kref_com_zhiniu_pages_components_StockFacts (*factsOf_)(libshared_kref_com_zhiniu_domain_model_StockQuote quote, libshared_kref_com_zhiniu_domain_model_StockFundamentals fundamentals);
              const char* (*fmtOptional)(libshared_kref_kotlin_Double value, const char* suffix);
              const char* (*fmtOptionalAmount)(libshared_kref_kotlin_Double value);
              libshared_kref_com_tencent_kuikly_core_base_Animation (*get_ANIM_THEME)();
              libshared_KFloat (*get_CONTENT_W)();
              libshared_KFloat (*get_PAD)();
              void (*cssClass)(libshared_kref_com_tencent_kuikly_core_base_Attr thiz, const char* value);
            } components;
          } pages;
          struct {
            struct {
              libshared_KType* (*_type)(void);
            } GatewayTransport;
            libshared_kref_com_zhiniu_platform_GatewayTransport (*createPlatformGatewayTransport)();
            const char* (*platformDefaultGateway)();
            void (*applyHostTheme)(libshared_KBoolean dark);
            libshared_KBoolean (*systemPrefersDark)();
            void (*watchSystemTheme)(libshared_kref_kotlin_Function1 callback);
          } platform;
        } zhiniu;
      } com;
      libshared_KInt (*initKuikly)();
    } root;
  } kotlin;
} libshared_ExportedSymbols;
extern libshared_ExportedSymbols* libshared_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBSHARED_H */
