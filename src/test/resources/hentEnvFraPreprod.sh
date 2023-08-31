kubectl config use-context dev-gcp
APP="tilleggsstonader-sak"
PODNAVN=$(kubectl -n tilleggsstonader get pods --field-selector=status.phase==Running -o name | grep $APP |  sed "s/^.\{4\}//" | head -n 1);

# echo "Henter variabler fra $PODNAVN"

PODVARIABLER="$(kubectl -n tilleggsstonader exec -c $APP -it "$PODNAVN" -- env)"
# echo "PODVARIABLER=$PODVARIABLER"

# TODO legg inn azure-ad
#TOKEN_X_CLIENT_ID="$(echo "$PODVARIABLER" | grep "TOKEN_X_CLIENT_ID" | tr -d '\r' )";
#TOKEN_X_WELL_KNOWN_URL="$(echo "$PODVARIABLER" | grep "TOKEN_X_WELL_KNOWN_URL" | tr -d '\r' )";
#TOKEN_X_PRIVATE_JWK="$(echo "$PODVARIABLER" | grep "TOKEN_X_PRIVATE_JWK" | tr -d '\r' )";
#
#if [[ -z "$TOKEN_X_CLIENT_ID" || -z "$TOKEN_X_WELL_KNOWN_URL" || -z "$TOKEN_X_PRIVATE_JWK" ]]
#then
#      echo "Fant ikke alle variabler"
#      exit 1
#fi
#echo "Envs:"
#echo "$TOKEN_X_WELL_KNOWN_URL"
#echo "$TOKEN_X_CLIENT_ID"
#echo "$TOKEN_X_PRIVATE_JWK"
