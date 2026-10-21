package app.hookbase.models;

import java.util.List;

/**
 * The source providers the API accepts today, as constants for {@code provider(...)}.
 *
 * <p>A list of suggestions, not a constraint: {@code provider} is a plain {@code String} on every
 * request and response, so any string is accepted and a source with a provider newer than this
 * SDK still parses. The API validates the value on create and update and answers an unknown one
 * with a 400 naming it.
 *
 * <p>Generated from {@code api/scripts/print-source-enums.ts} (via the Python SDK's
 * {@code KNOWN_SOURCE_PROVIDERS}, cross-checked against Node's {@code KnownSourceProvider}): it
 * mirrors {@code SUPPORTED_SIGNATURE_PROVIDERS} in the API, 71 signature schemes plus the
 * aliases {@code svix}, {@code clerk}, {@code resend}, {@code render}, {@code etsy},
 * {@code stytch} (all of {@code standard-webhooks}) and {@code generic} (of {@code custom}).
 * Regenerate with {@code npx tsx scripts/print-source-enums.ts} in the api package.
 */
public final class KnownSourceProviders {
    private KnownSourceProviders() {}

    public static final String ADYEN = "adyen";
    public static final String AIRTABLE = "airtable";
    public static final String ASANA = "asana";
    public static final String AUTH0 = "auth0";
    public static final String AWS_SNS = "aws-sns";
    public static final String BIGCOMMERCE = "bigcommerce";
    public static final String BITBUCKET = "bitbucket";
    public static final String BRAINTREE = "braintree";
    public static final String CALENDLY = "calendly";
    public static final String CHARGEBEE = "chargebee";
    public static final String CIRCLECI = "circleci";
    public static final String CLERK = "clerk";
    public static final String CLICKUP = "clickup";
    public static final String CLOUDFLARE = "cloudflare";
    public static final String COINBASE_COMMERCE = "coinbase-commerce";
    public static final String CUSTOM = "custom";
    public static final String DATADOG = "datadog";
    public static final String DISCORD = "discord";
    public static final String DOCUSIGN = "docusign";
    public static final String ETSY = "etsy";
    public static final String FRESHDESK = "freshdesk";
    public static final String GENERIC = "generic";
    public static final String GITHUB = "github";
    public static final String GITLAB = "gitlab";
    public static final String GITLAB_SIGNING_TOKEN = "gitlab-signing-token";
    public static final String GOCARDLESS = "gocardless";
    public static final String GRAFANA = "grafana";
    public static final String GRAFANA_HMAC = "grafana-hmac";
    public static final String GRAFANA_HMAC_TIMESTAMPED = "grafana-hmac-timestamped";
    public static final String HEROKU = "heroku";
    public static final String HUBSPOT = "hubspot";
    public static final String INTERCOM = "intercom";
    public static final String JIRA = "jira";
    public static final String KLAVIYO = "klaviyo";
    public static final String LAUNCHDARKLY = "launchdarkly";
    public static final String LEMONSQUEEZY = "lemonsqueezy";
    public static final String LINEAR = "linear";
    public static final String MAILCHIMP = "mailchimp";
    public static final String MAILGUN = "mailgun";
    public static final String MOLLIE = "mollie";
    public static final String NETLIFY = "netlify";
    public static final String NEWRELIC = "newrelic";
    public static final String NOTION = "notion";
    public static final String OKTA = "okta";
    public static final String PADDLE = "paddle";
    public static final String PAGERDUTY = "pagerduty";
    public static final String PAYPAL = "paypal";
    public static final String PIPEDRIVE = "pipedrive";
    public static final String POSTMARK = "postmark";
    public static final String RAILWAY = "railway";
    public static final String RAZORPAY = "razorpay";
    public static final String RENDER = "render";
    public static final String RESEND = "resend";
    public static final String SEGMENT = "segment";
    public static final String SENDGRID = "sendgrid";
    public static final String SENTRY = "sentry";
    public static final String SHOPIFY = "shopify";
    public static final String SLACK = "slack";
    public static final String SQUARE = "square";
    public static final String STANDARD_WEBHOOKS = "standard-webhooks";
    public static final String STRIPE = "stripe";
    public static final String STYTCH = "stytch";
    public static final String SUPABASE = "supabase";
    public static final String SUPABASE_AUTH_HOOKS = "supabase-auth-hooks";
    public static final String SVIX = "svix";
    public static final String TELEGRAM = "telegram";
    public static final String TERRAFORM_CLOUD = "terraform-cloud";
    public static final String TRELLO = "trello";
    public static final String TWILIO = "twilio";
    public static final String TYPEFORM = "typeform";
    public static final String VERCEL = "vercel";
    public static final String WHATSAPP = "whatsapp";
    public static final String WISE = "wise";
    public static final String WISE_SANDBOX = "wise-sandbox";
    public static final String WOOCOMMERCE = "woocommerce";
    public static final String WORKOS = "workos";
    public static final String ZENDESK = "zendesk";
    public static final String ZOOM = "zoom";

    /** Every known provider, in alphabetical order (78 of them). */
    public static final List<String> ALL = List.of(
            ADYEN,
            AIRTABLE,
            ASANA,
            AUTH0,
            AWS_SNS,
            BIGCOMMERCE,
            BITBUCKET,
            BRAINTREE,
            CALENDLY,
            CHARGEBEE,
            CIRCLECI,
            CLERK,
            CLICKUP,
            CLOUDFLARE,
            COINBASE_COMMERCE,
            CUSTOM,
            DATADOG,
            DISCORD,
            DOCUSIGN,
            ETSY,
            FRESHDESK,
            GENERIC,
            GITHUB,
            GITLAB,
            GITLAB_SIGNING_TOKEN,
            GOCARDLESS,
            GRAFANA,
            GRAFANA_HMAC,
            GRAFANA_HMAC_TIMESTAMPED,
            HEROKU,
            HUBSPOT,
            INTERCOM,
            JIRA,
            KLAVIYO,
            LAUNCHDARKLY,
            LEMONSQUEEZY,
            LINEAR,
            MAILCHIMP,
            MAILGUN,
            MOLLIE,
            NETLIFY,
            NEWRELIC,
            NOTION,
            OKTA,
            PADDLE,
            PAGERDUTY,
            PAYPAL,
            PIPEDRIVE,
            POSTMARK,
            RAILWAY,
            RAZORPAY,
            RENDER,
            RESEND,
            SEGMENT,
            SENDGRID,
            SENTRY,
            SHOPIFY,
            SLACK,
            SQUARE,
            STANDARD_WEBHOOKS,
            STRIPE,
            STYTCH,
            SUPABASE,
            SUPABASE_AUTH_HOOKS,
            SVIX,
            TELEGRAM,
            TERRAFORM_CLOUD,
            TRELLO,
            TWILIO,
            TYPEFORM,
            VERCEL,
            WHATSAPP,
            WISE,
            WISE_SANDBOX,
            WOOCOMMERCE,
            WORKOS,
            ZENDESK,
            ZOOM);
}
